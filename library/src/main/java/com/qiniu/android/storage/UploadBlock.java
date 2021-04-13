package com.qiniu.android.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class UploadBlock {

    final long offset;
    final long size;
    final int index;
    final ArrayList<UploadData> uploadDataList;

    String context;

    UploadBlock(long offset, long blockSize, int dataSize, int index) {
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

    static UploadBlock blockFromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
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
                if (data != null) {
                    uploadDataList.add(data);
                }
            }
        } catch (JSONException e) {
        }
        ;
        UploadBlock block = new UploadBlock(offset, size, index, uploadDataList);
        if (context != null && context.length() > 0) {
            block.context = context;
        }
        return block;
    }

    boolean isCompleted() {
        if (uploadDataList == null) {
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

    private ArrayList<UploadData> createDataList(int dataSize) {
        long offset = 0;
        int dataIndex = 1;
        ArrayList<UploadData> datas = new ArrayList<UploadData>();
        while (offset < size) {
            long lastSize = size - offset;
            int dataSizeP = Math.min((int) lastSize, dataSize);
            UploadData data = new UploadData(offset, dataSizeP, dataIndex);
            if (data != null) {
                datas.add(data);
                offset += dataSizeP;
                dataIndex += 1;
            }
        }
        return datas;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("offset", offset);
            jsonObject.put("size", size);
            jsonObject.put("index", index);
            jsonObject.put("context", (context != null ? context : ""));
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

    protected UploadData nextUploadData() {
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

    protected void clearUploadState() {
        context = null;
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return;
        }
        for (UploadData data : uploadDataList) {
            data.clearUploadState();
        }
    }
}
