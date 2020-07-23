package com.qiniu.android.http.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UploadFileInfo {

    public final long size;
    public final long modifyTime;
    public final ArrayList<UploadBlock> uploadBlocks;

    private UploadFileInfo(long size,
                           long modifyTime,
                           ArrayList<UploadBlock> uploadBlocks) {
        this.size = size;
        this.modifyTime = modifyTime;
        this.uploadBlocks = uploadBlocks;
    }

    public UploadFileInfo(long fileSize,
                          long blockSize,
                          long dataSize,
                          long modifyTime){
        this.size = fileSize;
        this.modifyTime = modifyTime;
        this.uploadBlocks =  createBlocks(blockSize, dataSize);
    }

    public static UploadFileInfo fileFromJson(JSONObject jsonObject){
        if (jsonObject == null){
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
                if (block != null){
                    uploadBlocks.add(block);
                }
            }
        } catch (JSONException e){};

        UploadFileInfo fileInfo = new UploadFileInfo(size, modifyTime, uploadBlocks);
        return fileInfo;
    }

    private ArrayList<UploadBlock> createBlocks(long blockSize,
                                                long dataSize){
        long offset = 0;
        int blockIndex = 0;
        ArrayList<UploadBlock> blocks = new ArrayList<>();
        while (offset < size){
            long lastSize = size - offset;
            long blockSizeP = Math.min(lastSize, blockSize);
            UploadBlock block = new UploadBlock(offset, blockSizeP, dataSize, blockIndex);
            if (block != null){
                blocks.add(block);
                offset += blockSizeP;
                blockIndex += 1;
            }
        }
        return blocks;
    }

    public double progress(){
        if (uploadBlocks == null || uploadBlocks.size() == 0){
            return 0;
        }
        double progress = 0;
        for (UploadBlock block : uploadBlocks) {
            progress += block.progress() * ((double) block.size / (double) size);
        }
        return progress;
    }

    public UploadData nextUploadData(){
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return null;
        }
        UploadData data = null;
        for (UploadBlock block : uploadBlocks) {
            data = block.nextUploadData();
            if (data != null) {
                break;
            }
        }
        return data;
    }

    public void clearUploadState(){
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return;
        }
        for (UploadBlock block : uploadBlocks) {
            block.clearUploadState();
        }
    }

    public UploadBlock blockWithIndex(int blockIndex){
        if (uploadBlocks == null || uploadBlocks.size() == 0) {
            return null;
        }
        UploadBlock block = null;
        if (blockIndex < uploadBlocks.size()) {
            block = uploadBlocks.get(blockIndex);
        }
        return block;
    }

    public boolean isAllUploaded(){
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

    public ArrayList<String> allBlocksContexts(){
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

    public JSONObject toJsonObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("size", size);
            jsonObject.put("modifyTime", modifyTime);
            if (uploadBlocks != null && uploadBlocks.size() > 0){
                JSONArray blockJsonArray = new JSONArray();
                for (UploadBlock block : uploadBlocks) {
                    JSONObject blockJson = block.toJsonObject();
                    if (blockJson != null){
                        blockJsonArray.put(blockJson);
                    }
                }
                jsonObject.put("uploadBlocks", blockJsonArray);
            }
        } catch (JSONException e) {}
        return jsonObject;
    }


    public static class UploadBlock {

        public final long offset;
        public final long size;
        public final int index;
        public final ArrayList<UploadData> uploadDataList;

        public String context;

        public UploadBlock(long offset,
                           long blockSize,
                           long dataSize,
                           int index) {
            this.offset = offset;
            this.size = blockSize;
            this.index = index;
            this.uploadDataList = createDataList(dataSize);
        }

        private UploadBlock(long offset,
                            long blockSize,
                            int index,
                            ArrayList<UploadData> uploadDataList) {
            this.offset = offset;
            this.size = blockSize;
            this.index = index;
            this.uploadDataList = uploadDataList;
        }

        public static UploadBlock blockFromJson(JSONObject jsonObject){
            if (jsonObject == null){
                return null;
            }
            long offset = 0;
            long size = 0;
            int index = 0;
            String context = null;
            ArrayList<UploadData> uploadDataList = new ArrayList<UploadData>();
            try {
                offset = jsonObject.getLong("offset");
                size = jsonObject.getLong("size");
                index = jsonObject.getInt("index");
                context = jsonObject.getString("context");
                JSONArray dataJsonArray = jsonObject.getJSONArray("uploadDataList");
                for (int i = 0; i < dataJsonArray.length(); i++) {
                    JSONObject dataJson = dataJsonArray.getJSONObject(i);
                    UploadData data = UploadData.dataFromJson(dataJson);
                    if (data != null){
                        uploadDataList.add(data);
                    }
                }
            } catch (JSONException e){};
            UploadBlock block = new UploadBlock(offset, size, index, uploadDataList);
            if (context != null && context.length() > 0){
                block.context = context;
            }
            return block;
        }

        public boolean isCompleted(){
            if (uploadDataList == null) {
                return true;
            }
            boolean isCompleted = true;
            for (UploadData data : uploadDataList) {
                if (!data.isCompleted){
                    isCompleted = false;
                    break;
                }
            }
            return isCompleted;
        }

        public double progress(){
            if (uploadDataList == null) {
                return 0;
            }
            double progress = 0;
            for (UploadData data : uploadDataList) {
                progress += data.progress * ((double) data.size / size);
            }
            return progress;
        }

        private ArrayList<UploadData> createDataList(long dataSize){
            long offset = 0;
            int dataIndex = 0;
            ArrayList<UploadData> datas = new ArrayList<UploadData>();
            while (offset < size){
                long lastSize = size - offset;
                long dataSizeP = Math.min(lastSize, dataSize);
                UploadData data = new UploadData(offset, dataSizeP, dataIndex, index);
                if (data != null){
                    datas.add(data);
                    offset += dataSizeP;
                    dataIndex += 1;
                }
            }
            return datas;
        }

        public JSONObject toJsonObject(){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("offset", offset);
                jsonObject.put("size", size);
                jsonObject.put("index", index);
                jsonObject.put("context", (context != null ? context : ""));
                if (uploadDataList != null && uploadDataList.size() > 0){
                    JSONArray dataJsonArray = new JSONArray();
                    for (UploadData data : uploadDataList) {
                        JSONObject dataJson = data.toJsonObject();
                        if (dataJson != null){
                            dataJsonArray.put(dataJson);
                        }
                    }
                    jsonObject.put("uploadDataList", dataJsonArray);
                }
            } catch (JSONException e) {}
            return jsonObject;
        }

        protected UploadData nextUploadData(){
            if (uploadDataList == null || uploadDataList.size() == 0){
                return null;
            }
            UploadData data = null;
            for (UploadData dataP : uploadDataList) {
                if (!dataP.isCompleted && !dataP.isUploading){
                    data = dataP;
                    break;
                }
            }
            return data;
        }

        protected void clearUploadState(){
            if (uploadDataList == null || uploadDataList.size() == 0){
                return;
            }
            for (UploadData data : uploadDataList) {
                data.clearUploadState();
            }
        }
    }

    public static class UploadData {

        public final long offset;
        public final long size;
        public final int index;
        public final int blockIndex;

        public boolean isCompleted;
        public boolean isUploading;
        public double progress;

        public UploadData(long offset,
                          long size,
                          int index,
                          int blockIndex) {
            this.offset = offset;
            this.size = size;
            this.index = index;
            this.blockIndex = blockIndex;
            this.isCompleted = false;
            this.isUploading = false;
            this.progress = 0;
        }

        public static UploadData dataFromJson(JSONObject jsonObject){
            if (jsonObject == null){
                return null;
            }
            long offset = 0;
            long size = 0;
            int index = 0;
            int blockIndex = 0;
            boolean isCompleted = false;
            double progress = 0;
            try {
                offset = jsonObject.getLong("offset");
                size = jsonObject.getLong("size");
                index = jsonObject.getInt("index");
                blockIndex = jsonObject.getInt("blockIndex");
                isCompleted = jsonObject.getBoolean("isCompleted");
                progress = jsonObject.getDouble("progress");
            } catch (JSONException ignored){}
            UploadData uploadData = new UploadData(offset, size, index, blockIndex);
            uploadData.isCompleted = isCompleted;
            uploadData.progress = progress;
            return uploadData;
        }

        public boolean isFirstData(){
            return index == 0;
        }

        public void clearUploadState(){
            isCompleted = false;
            isUploading = false;
        }

        private JSONObject toJsonObject(){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("offset", offset);
                jsonObject.put("size", size);
                jsonObject.put("index", index);
                jsonObject.put("blockIndex", blockIndex);
                jsonObject.put("isCompleted", isCompleted);
                jsonObject.put("progress", progress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }
}
