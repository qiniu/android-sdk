package com.qiniu.android.storage;

import com.qiniu.android.utils.BytesUtils;
import com.qiniu.android.utils.Etag;
import com.qiniu.android.utils.MD5;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class UploadInfoV1 extends UploadInfo {

    private static final String TypeKey = "infoType";
    private static final String TypeValue = "UploadInfoV1";
    private static final int BlockSize = 4 * 1024 * 1024;

    private final int dataSize;
    private List<UploadBlock> blockList;

    private boolean isEOF = false;
    private IOException readException = null;

    private UploadInfoV1(UploadSource source, int dataSize, List<UploadBlock> blockList) {
        super(source);
        this.dataSize = dataSize;
        this.blockList = blockList;
    }

    UploadInfoV1(UploadSource source, Configuration configuration) {
        super(source, configuration);
        if (configuration.useConcurrentResumeUpload || configuration.chunkSize > BlockSize) {
            this.dataSize = BlockSize;
        } else {
            this.dataSize = configuration.chunkSize;
        }
        this.blockList = new ArrayList<>();
    }

    static UploadInfoV1 infoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        int dataSize = 0;
        String type = null;
        List<UploadBlock> blockList = new ArrayList<>();
        try {
            type = jsonObject.optString(TypeKey);
            dataSize = jsonObject.getInt("dataSize");
            JSONArray blockJsonArray = jsonObject.getJSONArray("blockList");
            for (int i = 0; i < blockJsonArray.length(); i++) {
                JSONObject blockJson = blockJsonArray.getJSONObject(i);
                UploadBlock block = UploadBlock.blockFromJson(blockJson);
                if (block != null) {
                    blockList.add(block);
                }
            }
        } catch (JSONException ignored) {
        }

        UploadInfoV1 info = new UploadInfoV1(source, dataSize, blockList);
        info.setInfoFromJson(jsonObject);
        if (!TypeValue.equals(type) || !source.getId().equals(info.getSourceId())) {
            return null;
        }

        return info;
    }

    @Override
    boolean reloadInfo() {
        isEOF = false;
        readException = null;
        return super.reloadInfo();
    }

    @Override
    boolean isSameUploadInfo(UploadInfo info) {
        if (!super.isSameUploadInfo(info)) {
            return false;
        }

        if (!(info instanceof UploadInfoV1)) {
            return false;
        }

        UploadInfoV1 infoV1 = (UploadInfoV1) info;
        return dataSize == infoV1.dataSize;
    }

    @Override
    void clearUploadState() {
        if (blockList == null || blockList.size() == 0) {
            return;
        }
        for (UploadBlock block : blockList) {
            block.clearUploadState();
        }
    }

    @Override
    long uploadSize() {
        if (blockList == null || blockList.size() == 0) {
            return 0;
        }
        long uploadSize = 0;
        for (UploadBlock block : blockList) {
            uploadSize += block.uploadSize();
        }
        return uploadSize;
    }

    // 文件已经读取结束 & 所有块均上传
    @Override
    boolean isAllUploaded() {
        if (!isEOF) {
            return false;
        }

        if (blockList == null || blockList.size() == 0) {
            return true;
        }
        boolean isAllUploaded = true;
        for (UploadBlock block : blockList) {
            if (!block.isCompleted()) {
                isAllUploaded = false;
                break;
            }
        }
        return isAllUploaded;
    }

    @Override
    JSONObject toJsonObject() {
        JSONObject jsonObject = super.toJsonObject();
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        try {
            jsonObject.put(TypeKey, TypeValue);
            jsonObject.put("dataSize", dataSize);
            if (blockList != null && blockList.size() > 0) {
                JSONArray blockJsonArray = new JSONArray();
                for (UploadBlock block : blockList) {
                    JSONObject blockJson = block.toJsonObject();
                    if (blockJson != null) {
                        blockJsonArray.put(blockJson);
                    }
                }
                jsonObject.put("blockList", blockJsonArray);
            }
        } catch (JSONException e) {
        }
        return jsonObject;
    }

    UploadBlock nextUploadBlock() throws IOException {

        // 从 blockList 中读取需要上传的 block
        UploadBlock block = null;
        while (true) {
            // 从 blockList 中读取需要上传的 block：不检测数据有效性
            block = nextUploadBlockFormBlockList();
            if (block == null) {
                break;
            }

            // 加载数据信息并检测数据有效性
            UploadBlock newBlock = loadBlockData(block);
            // 根据 block 未加载到数据, block 数据是无效的
            if (newBlock == null) {
                isEOF = true;
                // 有多余的 block 则移除，包含 block
                if (blockList.size() > block.index) {
                    blockList = blockList.subList(0, block.index);
                }
                block = null;
                break;
            }

            // 加载到数据
            // 加载到数据不符合预期，更换 block 信息
            if (newBlock != block) {
                blockList.set(newBlock.index, newBlock);
            }

            // 数据读取结束
            if (newBlock.size < BlockSize) {
                // 有多余的 block 则移除，不包含 newBlock
                if (blockList.size() > newBlock.index + 1) {
                    blockList = blockList.subList(0, newBlock.index + 1);
                }
                isEOF = true;
            }

            block = newBlock;

            // 数据需要上传
            if (block.nextUploadDataWithoutCheckData() != null) {
                break;
            }
        }

        if (block != null) {
            return block;
        }

        // 内存的 blockList 中没有可上传的数据，则从资源中读并创建 block
        // 资源读取异常，不可读取
        if (readException != null) {
            throw readException;
        }

        // 资源已经读取完毕，不能再读取
        if (isEOF) {
            return null;
        }

        // 从资源中读取新的 block 进行上传
        long blockOffset = 0;
        if (blockList.size() > 0) {
            UploadBlock lastBlock = blockList.get(blockList.size() - 1);
            blockOffset = lastBlock.offset + lastBlock.size;
        }

        block = new UploadBlock(blockOffset, BlockSize, dataSize, blockList.size());
        block = loadBlockData(block);
        // 资源 EOF
        if (block == null || block.size < BlockSize) {
            isEOF = true;
        }

        // 读到 block,由于是新数据，则必定为需要上传的数据
        if (block != null) {
            block.updateDataState(UploadData.State.WaitToUpload);
            blockList.add(block);
        }

        return block;
    }

    private UploadBlock nextUploadBlockFormBlockList() {
        if (blockList == null || blockList.size() == 0) {
            return null;
        }
        UploadBlock block = null;
        for (UploadBlock blockP : blockList) {
            UploadData data = blockP.nextUploadDataWithoutCheckData();
            if (data != null) {
                block = blockP;
                break;
            }
        }
        return block;
    }


    // 加载块中的数据
    // 1. 数据块已加载，直接返回
    // 2. 数据块未加载，读块数据
    // 2.1 如果未读到数据，则已 EOF，返回 null
    // 2.2 如果读到数据
    // 2.2.1 如果块数据符合预期，则当片未上传，则加载片数据
    // 2.2.2 如果块数据不符合预期，创建新块，加载片信息
    private UploadBlock loadBlockData(UploadBlock block) throws IOException {
        if (block == null) {
            return null;
        }

        // 已经加载过 block 数据
        // 没有需要上传的片 或者 有需要上传片但是已加载过片数据
        UploadData nextUploadData = block.nextUploadDataWithoutCheckData();
        if (nextUploadData.getState() == UploadData.State.WaitToUpload) {
            return block;
        }

        // 未加载过 block 数据
        // 根据 block 信息加载 blockBytes
        byte[] blockBytes = null;
        try {
            blockBytes = readData(block.size, block.offset);
        } catch (IOException e) {
            readException = e;
            throw e;
        }

        // 没有数据不需要上传
        if (blockBytes == null || blockBytes.length == 0) {
            return null;
        }

        String md5 = MD5.encrypt(blockBytes);
        // 判断当前 block 的数据是否和实际数据吻合，不吻合则之前 block 被抛弃，重新创建 block
        if (blockBytes.length != block.size || block.md5 == null || !block.md5.equals(md5)) {
            block = new UploadBlock(block.offset, blockBytes.length, dataSize, block.index);
            block.md5 = md5;
        }

        for (UploadData data : block.uploadDataList) {
            if (StringUtils.isNullOrEmpty(data.ctx)) {
                // 还未上传的
                try {
                    data.data = BytesUtils.subBytes(blockBytes, (int) data.offset, data.size);
                    data.updateState(UploadData.State.WaitToUpload);
                } catch (IOException e) {
                    readException = e;
                    throw e;
                }
            } else {
                // 已经上传的
                data.updateState(UploadData.State.Complete);
            }
        }

        return block;
    }

    UploadData nextUploadData(UploadBlock block) throws IOException {
        if (block == null) {
            return null;
        }
        return block.nextUploadDataWithoutCheckData();
    }

    ArrayList<String> allBlocksContexts() {
        if (blockList == null || blockList.size() == 0) {
            return null;
        }
        ArrayList<String> contexts = new ArrayList<String>();
        for (UploadBlock block : blockList) {
            String ctx = block.getUploadContext();
            if (!StringUtils.isNullOrEmpty(ctx)) {
                contexts.add(ctx);
            }
        }
        return contexts;
    }
}
