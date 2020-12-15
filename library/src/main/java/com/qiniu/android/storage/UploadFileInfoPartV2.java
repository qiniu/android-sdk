package com.qiniu.android.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFileInfoPartV2 extends UploadFileInfo {

    final ArrayList<UploadData> uploadDataList;

    String uploadId;
    // 单位：
    Long expireAt;

    private UploadFileInfoPartV2(long size,
                                 long modifyTime,
                                 ArrayList<UploadData> uploadDataList) {
        super(size, modifyTime);
        this.uploadDataList = uploadDataList;
    }

    UploadFileInfoPartV2(long size,
                         long dataSize,
                         long modifyTime) {
        super(size, modifyTime);
        this.uploadDataList = createDataList(dataSize);
    }

    UploadFileInfoPartV2 fileFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
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
                if (data != null) {
                    uploadDataList.add(data);
                }
            }
        } catch (JSONException e) {
        }

        UploadFileInfoPartV2 fileInfo = new UploadFileInfoPartV2(size, modifyTime, uploadDataList);
        fileInfo.expireAt = expireAt;
        fileInfo.uploadId = uploadId;
        return fileInfo;
    }

    private ArrayList<UploadData> createDataList(long dataSize) {
        long offset = 0;
        int dataIndex = 1;
        ArrayList<UploadData> dataList = new ArrayList<UploadData>();
        while (offset < size) {
            long lastSize = size - offset;
            long dataSizeP = Math.min(lastSize, dataSize);
            UploadData data = new UploadData(offset, dataSizeP, dataIndex);
            if (data != null) {
                dataList.add(data);
                offset += dataSizeP;
                dataIndex += 1;
            }
        }
        return dataList;
    }

    double progress() {
        if (uploadDataList == null) {
            return 0;
        }
        double progress = 0;
        for (UploadData data : uploadDataList) {
            progress += data.progress * ((double) data.size / size);
        }
        return progress;
    }

    @Override
    boolean isEmpty() {
        return uploadDataList == null || uploadDataList.size() == 0;
    }

    @Override
    boolean isValid() {
        return !isEmpty() && uploadId != null && (expireAt - new Date().getTime() * 0.001) > 6000;
    }

    UploadData nextUploadData() {
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return null;
        }
        UploadData data = null;
        for (UploadData dataP : uploadDataList) {
            if (!dataP.isCompleted && !dataP.isUploading) {
                data = dataP;
                break;
            }
        }
        return data;
    }

    void clearUploadState() {
        for (UploadData data : uploadDataList) {
            data.clearUploadState();
        }
    }

    boolean isAllUploaded() {
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return true;
        }
        boolean isCompleted = true;
        for (UploadData data : uploadDataList) {
            if (!data.isCompleted) {
                isCompleted = false;
                break;
            }
        }
        return isCompleted;
    }

    List<Map<String, Object>> getPartInfoArray() {
        if (uploadId == null || uploadId.length() == 0) {
            return null;
        }
        ArrayList<Map<String, Object>> infoArray = new ArrayList<>();
        for (UploadData data : uploadDataList) {
            if (data.etag != null) {
                HashMap<String, Object> info = new HashMap<>();
                info.put("etag", data.etag);
                info.put("partNumber", data.index);
                infoArray.add(info);
            }
        }
        return infoArray;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("size", size);
            jsonObject.put("modifyTime", modifyTime);
            jsonObject.put("expireAt", expireAt);
            jsonObject.put("uploadId", uploadId);
            if (uploadDataList != null && uploadDataList.size() > 0) {
                JSONArray dataJsonArray = new JSONArray();
                for (UploadData data : uploadDataList) {
                    JSONObject dataJson = data.toJsonObject();
                    if (dataJson != null) {
                        dataJsonArray.put(dataJson);
                    }
                }
                jsonObject.put("uploadDataList", dataJsonArray);
            }
        } catch (JSONException e) {
        }
        return jsonObject;
    }
}
