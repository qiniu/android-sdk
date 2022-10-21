package com.qiniu.android.storage;

import com.qiniu.android.utils.BytesUtils;
import com.qiniu.android.utils.ListVector;
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
    private static final int BlockListCapacityIncrement = 2;

    private final int dataSize;
    private ListVector<UploadBlock> blockList;

    private boolean isEOF = false;
    private IOException readException = null;

    private UploadInfoV1(UploadSource source, int dataSize, ListVector<UploadBlock> blockList) {
        super(source);
        this.dataSize = dataSize;
        this.blockList = blockList;
    }

    UploadInfoV1(UploadSource source, Configuration configuration) {
        super(source);
        if (configuration.useConcurrentResumeUpload || configuration.chunkSize > BlockSize) {
            this.dataSize = BlockSize;
        } else {
            this.dataSize = configuration.chunkSize;
        }
        this.blockList = new ListVector<>(BlockListCapacityIncrement, BlockListCapacityIncrement);
    }

    static UploadInfoV1 infoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        int dataSize = 0;
        String type = null;
        ListVector<UploadBlock> blockList = null;
        try {
            type = jsonObject.optString(TypeKey);
            dataSize = jsonObject.getInt("dataSize");
            JSONArray blockJsonArray = jsonObject.getJSONArray("blockList");
            blockList = new ListVector<>(blockJsonArray.length(), BlockListCapacityIncrement);
            for (int i = 0; i < blockJsonArray.length(); i++) {
                JSONObject blockJson = blockJsonArray.getJSONObject(i);
                try {
                    UploadBlock block = UploadBlock.blockFromJson(blockJson);
                    if (block != null) {
                        blockList.add(block);
                    }
                } catch (Exception ignore) {
                    break;
                }
            }
        } catch (JSONException ignored) {
            return null;
        }

        UploadInfoV1 info = new UploadInfoV1(source, dataSize, blockList);
        info.setInfoFromJson(jsonObject);
        if (!TypeValue.equals(type) || !source.getId().equals(info.getSourceId())) {
            return null;
        }

        return info;
    }

    boolean isFirstData(UploadData data) {
        return data.index == 0;
    }

    @Override
    boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        final boolean[] isValid = {true};
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                boolean valid = block.isValid();
                if (!valid) {
                    isValid[0] = false;
                    return true;
                }
                return false;
            }
        });
        return isValid[0];
    }

    @Override
    boolean reloadSource() {
        isEOF = false;
        readException = null;
        return super.reloadSource();
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

        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                block.clearUploadState();
                return false;
            }
        });
    }

    @Override
    long uploadSize() {
        if (blockList == null || blockList.size() == 0) {
            return 0;
        }
        final long[] uploadSize = {0};
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                uploadSize[0] += block.uploadSize();
                return false;
            }
        });
        return uploadSize[0];
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
        final boolean[] isAllUploaded = {true};
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                if (!block.isCompleted()) {
                    isAllUploaded[0] = false;
                    return true;
                } else {
                    return false;
                }
            }
        });
        return isAllUploaded[0];
    }

    @Override
    void checkInfoStateAndUpdate() {
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                block.checkInfoStateAndUpdate();
                return false;
            }
        });
    }

    @Override
    JSONObject toJsonObject() {
        JSONObject jsonObject = super.toJsonObject();
        if (jsonObject == null) {
            return null;
        }
        try {
            jsonObject.put(TypeKey, TypeValue);
            jsonObject.put("dataSize", dataSize);
            if (blockList != null && blockList.size() > 0) {
                final JSONArray blockJsonArray = new JSONArray();
                blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
                    @Override
                    public boolean enumerate(UploadBlock block) {
                        try {
                            JSONObject blockJson = block.toJsonObject();
                            if (blockJson != null) {
                                blockJsonArray.put(blockJson);
                            }
                        } catch (Exception e) {
                            return true;
                        }
                        return false;
                    }
                });

                if (blockJsonArray.length() != blockList.size()) {
                    return null;
                }
                jsonObject.put("blockList", blockJsonArray);
            }
        } catch (Exception e) {
            return null;
        }
        return jsonObject;
    }

    UploadBlock nextUploadBlock() throws IOException {

        // 从 blockList 中读取需要上传的 block
        UploadBlock block = nextUploadBlockFormBlockList();

        // 内存的 blockList 中没有可上传的数据，则从资源中读并创建 block
        if (block == null) {
            if (isEOF) {
                return null;
            } else if (readException != null) {
                // 资源读取异常，不可读取
                throw readException;
            }

            // 从资源中读取新的 block 进行上传
            long blockOffset = 0;
            if (blockList.size() > 0) {
                UploadBlock lastBlock = blockList.get(blockList.size() - 1);
                blockOffset = lastBlock.offset + lastBlock.size;
            }
            block = new UploadBlock(blockOffset, BlockSize, dataSize, blockList.size());
        }

        UploadBlock loadBlock = null;
        try {
            loadBlock = loadBlockData(block);
        } catch (IOException e) {
            readException = e;
            throw e;
        }

        if (loadBlock == null) {
            // 没有加在到 block, 也即数据源读取结束
            isEOF = true;
            // 有多余的 block 则移除，移除中包含 block
            if (blockList.size() > block.index) {
                blockList = blockList.subList(0, block.index);
            }
        } else {
            // 加在到 block
            if (loadBlock.index == blockList.size()) {
                // 新块：block index 等于 blockList size 则为新创建 block，需要加入 blockList
                blockList.add(loadBlock);
            } else if (loadBlock != block) {
                // 更换块：重新加在了 block， 更换信息
                blockList.set(loadBlock.index, loadBlock);
            }

            // 数据源读取结束，块读取大小小于预期，读取结束
            if (loadBlock.size < BlockSize) {
                isEOF = true;
                // 有多余的 block 则移除，移除中不包含 block
                if (blockList.size() > block.index + 1) {
                    blockList = blockList.subList(0, block.index + 1);
                }
            }
        }

        return loadBlock;
    }

    private UploadBlock nextUploadBlockFormBlockList() {
        if (blockList == null || blockList.size() == 0) {
            return null;
        }
        final UploadBlock[] blocks = {null};
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                UploadData data = block.nextUploadDataWithoutCheckData();
                if (data != null) {
                    blocks[0] = block;
                    return true;
                } else {
                    return false;
                }
            }
        });
        return blocks[0];
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
        if (nextUploadData.getState() == UploadData.State.WaitToUpload && nextUploadData.data != null) {
            return block;
        }

        // 未加载过 block 数据
        // 根据 block 信息加载 blockBytes
        byte[] blockBytes = null;
        try {
            blockBytes = readData(block.size, block.offset);
        } catch (IOException e) {
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
            if (data.getState() != UploadData.State.Complete) {
                // 还未上传的
                try {
                    data.data = BytesUtils.subBytes(blockBytes, (int) data.offset, data.size);
                    data.updateState(UploadData.State.WaitToUpload);
                } catch (IOException e) {
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
        final ArrayList<String> contexts = new ArrayList<String>();
        blockList.enumerateObjects(new ListVector.EnumeratorHandler<UploadBlock>() {
            @Override
            public boolean enumerate(UploadBlock block) {
                String ctx = block.ctx;
                if (!StringUtils.isNullOrEmpty(ctx)) {
                    contexts.add(ctx);
                }
                return false;
            }
        });
        return contexts;
    }
}
