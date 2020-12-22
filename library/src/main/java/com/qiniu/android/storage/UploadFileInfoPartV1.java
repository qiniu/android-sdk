package com.qiniu.android.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class UploadFileInfoPartV1 extends UploadFileInfo {

    final ArrayList<UploadBlock> uploadBlocks;

    UploadFileInfoPartV1(long size,
                         long modifyTime,
                         ArrayList<UploadBlock> uploadBlocks) {
        super(size, modifyTime);
        this.uploadBlocks = uploadBlocks;
    }

    UploadFileInfoPartV1(long size,
                         long blockSize,
                         long dataSize,
                         long modifyTime) {
        super(size, modifyTime);
        this.uploadBlocks = createBlocks(blockSize, dataSize);
    }

    static UploadFileInfoPartV1 fileFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        long size = 0;
        long modifyTime = 0;
        ArrayList<UploadBlock> uploadBlocks = new ArrayList<UploadBlock>();
        try {
            size = jsonObject.getLong("size");
            modifyTime = jsonObject.getLong("modifyTime");
            JSONArray blockJsonArray = jsonObject.getJSONArray("uploadBlocks");
            for (int i = 0; i < blockJsonArray.length(); i++) {
                JSONObject blockJson = blockJsonArray.getJSONObject(i);
                UploadBlock block = UploadBlock.blockFromJson(blockJson);
                if (block != null) {
                    uploadBlocks.add(block);
                }
            }
        } catch (JSONException e) {
        }

        UploadFileInfoPartV1 fileInfo = new UploadFileInfoPartV1(size, modifyTime, uploadBlocks);
        return fileInfo;
    }

    private ArrayList<UploadBlock> createBlocks(long blockSize,
                                                long dataSize) {
        long offset = 0;
        int blockIndex = 0;
        ArrayList<UploadBlock> blocks = new ArrayList<>();
        while (offset < size) {
            long lastSize = size - offset;
            long blockSizeP = Math.min(lastSize, blockSize);
            UploadBlock block = new UploadBlock(offset, blockSizeP, dataSize, blockIndex);
            if (block != null) {
                blocks.add(block);
                offset += blockSizeP;
                blockIndex += 1;
            }
        }
        return blocks;
    }

    double progress() {
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return 0;
        }
        double progress = 0;
        for (UploadBlock block : uploadBlocks) {
            progress += block.progress() * ((double) block.size / (double) size);
        }
        return progress;
    }

    @Override
    boolean isEmpty() {
        return uploadBlocks == null || uploadBlocks.size() == 0;
    }

    @Override
    boolean isValid() {
        return !isEmpty();
    }

    UploadBlock nextUploadBlock() {
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return null;
        }
        UploadBlock block = null;
        for (UploadBlock blockP : uploadBlocks) {
            UploadData data = blockP.nextUploadData();
            if (data != null) {
                block = blockP;
                break;
            }
        }
        return block;
    }

    void clearUploadState() {
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return;
        }
        for (UploadBlock block : uploadBlocks) {
            block.clearUploadState();
        }
    }

    boolean isAllUploaded() {
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return true;
        }
        boolean isAllUploaded = true;
        for (UploadBlock block : uploadBlocks) {
            if (!block.isCompleted()) {
                isAllUploaded = false;
                break;
            }
        }
        return isAllUploaded;
    }

    ArrayList<String> allBlocksContexts() {
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return null;
        }
        ArrayList<String> contexts = new ArrayList<String>();
        for (UploadBlock block : uploadBlocks) {
            if (block.context != null) {
                contexts.add(block.context);
            }
        }
        return contexts;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("size", size);
            jsonObject.put("modifyTime", modifyTime);
            if (uploadBlocks != null && uploadBlocks.size() > 0) {
                JSONArray blockJsonArray = new JSONArray();
                for (UploadBlock block : uploadBlocks) {
                    JSONObject blockJson = block.toJsonObject();
                    if (blockJson != null) {
                        blockJsonArray.put(blockJson);
                    }
                }
                jsonObject.put("uploadBlocks", blockJsonArray);
            }
        } catch (JSONException e) {
        }
        return jsonObject;
    }
}

