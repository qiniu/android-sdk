package com.qiniu.android.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class UploadInfoV1 extends UploadInfo {

    private static int BlockSize = 4 * 1024 * 1024;

    private int dataSize = 0;
    private boolean isEOF = false;
    private List<UploadBlock> blockList = new ArrayList<>();

    private UploadInfoV1() {
    }

    UploadInfoV1(UploadSource source, Configuration configuration) {
        super(source, configuration);
        if (configuration.useConcurrentResumeUpload || configuration.chunkSize > BlockSize) {
            this.dataSize = BlockSize;
        } else {
            this.dataSize = configuration.chunkSize;
        }

        blockList = createBlockList(BlockSize, this.dataSize);
    }

    static UploadInfoV1 infoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        long size = 0;
        int dataSize = 0;
        String sourceId = null;
        List<UploadBlock> blockList = new ArrayList<UploadBlock>();
        try {
            size = jsonObject.getLong("size");
            dataSize = jsonObject.getInt("dataSize");
            sourceId = jsonObject.optString("sourceId");
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

        UploadInfoV1 info = new UploadInfoV1();
        info.fileSize = size;
        info.dataSize = dataSize;
        info.sourceId = sourceId;
        info.blockList = blockList;
        info.setSource(source);
        return info;
    }

    @Override
    boolean isSameUploadInfo(UploadInfo info) {
        if (!super.isSameUploadInfo(info)) {
            return false;
        }

        if (!(info instanceof UploadInfoV1)) {
            return false;
        }

        UploadInfoV1 infoV1 = (UploadInfoV1)info;
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
    double progress() {
        if (blockList == null || blockList.size() == 0 || fileSize < 0) {
            return 0;
        }
        double progress = 0;
//        for (UploadBlock block : blocks) {
//            progress += block.progress() * ((double) block.size / (double) size);
//        }
        return progress;
    }

    @Override
    boolean isAllUploadingOrUploaded() {
        return false;
    }

    @Override
    boolean isAllUploaded() {
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
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("size", fileSize);
            jsonObject.put("dataSize", dataSize);
            jsonObject.put("sourceId", sourceId);
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
        // 1. 从内存的 blockList 中读取需要上传的 block
        UploadBlock block = nextUploadBlockFormBlockList();
        if (block != null) {
            return block;
        }

        // 2. 资源已经读取完毕，不能再读取
        if (isEOF) {
            return null;
        }

        // 3. 从资源中读取新的 block 进行上传
        long blockOffset = 0;

        if (blockList.size() > 0) {
            UploadBlock lastBlock = blockList.get(blockList.size() - 1);
            blockOffset = lastBlock.offset + lastBlock.size;
        }

        int dataIndex = 0; // 片在块中的 index
        int dataOffSize = 0; // 片在块中的偏移量
        List<UploadData> dataList = new ArrayList<>();
        while (dataOffSize < BlockSize && !isEOF) {
            // 获取片大小，块中所有片的总和必须为 BlockSize
            int dataSize = Math.min(this.dataSize, BlockSize - dataOffSize);

            // 读取片数据
            byte[] dataBytes = readData(dataSize, blockOffset + dataOffSize);
            // 片数据大小不符合预期说明已经读到文件结尾
            if (dataBytes.length < dataSize) {
                dataSize = dataBytes.length;
                isEOF = true;
            }
            // 未读到数据不必构建片模型
            if (dataSize == 0) {
                break;
            }

            // 构造片模型
            UploadData data = new UploadData(dataOffSize, dataSize, dataIndex);
            data.data = dataBytes;
            dataList.add(data);

            dataIndex += 1;
            dataOffSize += dataSize;
        }

        // 没有读到片数据 不必构建块模型
        if (dataList.size() == 0) {
            return null;
        }

        // 构建块模型
        long blockSize = dataOffSize;
        int blockIndex = blockList.size();
        block = new UploadBlock(blockOffset, blockSize, blockIndex, dataList);
        blockList.add(block);

        return block;
    }

    private UploadBlock nextUploadBlockFormBlockList() {
        if (blockList == null || blockList.size() == 0) {
            return null;
        }
        UploadBlock block = null;
        for (UploadBlock blockP : blockList) {
            UploadData data = blockP.nextUploadData();
            if (data != null) {
                block = blockP;
                break;
            }
        }
        return block;
    }

    UploadData nextUploadData(UploadBlock block) throws IOException {
        UploadData data = block.nextUploadData();
        // 当知道 size 提前创建块信息 和 从本地恢复数据时存在 没有 data 数据的情况
        if (data.data == null) {
            data.data = readData(data.size, block.offset + data.offset);
        }
        return data;
    }

    ArrayList<String> allBlocksContexts() {
        if (blockList == null || blockList.size() == 0) {
            return null;
        }
        ArrayList<String> contexts = new ArrayList<String>();
        for (UploadBlock block : blockList) {
            if (block.context != null) {
                contexts.add(block.context);
            }
        }
        return contexts;
    }

    private List<UploadBlock> createBlockList(int blockSize, int dataSize) {
        List<UploadBlock> blockList = new ArrayList<>();
        if (fileSize < 0) {
            return blockList;
        }

        long offset = 0;
        int blockIndex = 0;
        while (offset < fileSize) {
            long lastSize = fileSize - offset;
            int blockSizeP = Math.min((int) lastSize, blockSize);
            UploadBlock block = new UploadBlock(offset, blockSizeP, dataSize, blockIndex);
            blockList.add(block);
            offset += blockSizeP;
            blockIndex += 1;
        }
        return blockList;
    }
}
