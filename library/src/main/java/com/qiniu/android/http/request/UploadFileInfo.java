package com.qiniu.android.http.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFileInfo {

    public final long size;
    public final long modifyTime;
    public final ArrayList<UploadData> uploadDataList;

    public String uploadId;
    public Long expireAt;

    private UploadFileInfo(long size,
                           long modifyTime,
                           ArrayList<UploadData> uploadDataList) {
        this.size = size;
        this.modifyTime = modifyTime;
        this.uploadDataList = uploadDataList;
    }

    public UploadFileInfo(long fileSize,
                          long dataSize,
                          long modifyTime){
        this.size = fileSize;
        this.modifyTime = modifyTime;
        this.uploadDataList =  createDataList(dataSize);
    }

    public static UploadFileInfo fileFromJson(JSONObject jsonObject){
        if (jsonObject == null){
            return null;
        }
        long size = 0;
        long modifyTime = 0;
        Long expireAt = null;
        String uploadId = null;
        ArrayList<UploadData> uploadDataList = new ArrayList<>();
        try {
            size = jsonObject.getLong("size");
            modifyTime = jsonObject.getLong("modifyTime");
            expireAt = jsonObject.getLong("expireAt");
            uploadId = jsonObject.getString("uploadId");
            JSONArray dataJsonArray = jsonObject.getJSONArray("uploadDataList");
            for (int i = 0; i < dataJsonArray.length(); i++) {
                JSONObject dataJson = dataJsonArray.getJSONObject(i);
                UploadData data = UploadData.dataFromJson(dataJson);
                if (data != null){
                    uploadDataList.add(data);
                }
            }
        } catch (JSONException e){};

        UploadFileInfo fileInfo = new UploadFileInfo(size, modifyTime, uploadDataList);
        fileInfo.expireAt = expireAt;
        fileInfo.uploadId = uploadId;
        return fileInfo;
    }

    private ArrayList<UploadData> createDataList(long dataSize){
        long offset = 0;
        int dataIndex = 1;
        ArrayList<UploadData> dataList = new ArrayList<UploadData>();
        while (offset < size){
            long lastSize = size - offset;
            long dataSizeP = Math.min(lastSize, dataSize);
            UploadData data = new UploadData(offset, dataSizeP, dataIndex);
            if (data != null){
                dataList.add(data);
                offset += dataSizeP;
                dataIndex += 1;
            }
        }
        return dataList;
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

    public UploadData nextUploadData(){
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

    public void clearUploadState(){
        for (UploadData data : uploadDataList) {
            data.clearUploadState();
        }
    }

    public boolean isAllUploaded(){
        if (uploadDataList == null || uploadDataList.size() == 0) {
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

    public List<Map<String, Object>> getPartInfoArray(){
        if (uploadId == null || uploadId.length() == 0) {
            return null;
        }
        ArrayList<Map<String, Object>> infoArray = new ArrayList<>();
        for (UploadData data : uploadDataList) {
            if (data.etag != null) {
                HashMap<String, Object>info = new HashMap<>();
                info.put("etag", data.etag);
                info.put("partNumber", data.index);
                infoArray.add(info);
            }
        }
        return infoArray;
    }

    public JSONObject toJsonObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("size", size);
            jsonObject.put("modifyTime", modifyTime);
            jsonObject.put("expireAt", expireAt);
            jsonObject.put("uploadId", uploadId);
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


    /*
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
                UploadData data = new UploadData(offset, dataSizeP, dataIndex);
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
*/
    public static class UploadData {

        public final long offset;
        public final long size;
        public final int index;

        public String etag;
        public boolean isCompleted;
        public boolean isUploading;
        public double progress;

        public byte[] data;

        public UploadData(long offset,
                          long size,
                          int index) {
            this.offset = offset;
            this.size = size;
            this.index = index;
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
            String etag = null;
            boolean isCompleted = false;
            double progress = 0;
            try {
                offset = jsonObject.getLong("offset");
                size = jsonObject.getLong("size");
                index = jsonObject.getInt("index");
                etag = jsonObject.getString("etag");
                isCompleted = jsonObject.getBoolean("isCompleted");
                progress = jsonObject.getDouble("progress");
            } catch (JSONException ignored){}
            UploadData uploadData = new UploadData(offset, size, index);
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
                jsonObject.put("etag", etag);
                jsonObject.put("isCompleted", isCompleted);
                jsonObject.put("progress", progress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        }
    }
}
