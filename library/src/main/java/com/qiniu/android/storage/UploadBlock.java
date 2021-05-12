package com.qiniu.android.storage;

import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class UploadBlock {

    final long offset;
    final int size;
    final int index;
    final List<UploadData> uploadDataList;

    String md5 = null;
    String ctx = null;

    UploadBlock(long offset, int blockSize, int dataSize, int index) {
        this.offset = offset;
        this.size = blockSize;
        this.index = index;
        this.uploadDataList = createDataList(dataSize);
    }

    UploadBlock(long offset, int blockSize, int index, List<UploadData> uploadDataList) {
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
        int size = 0;
        int index = 0;
        String md5 = null;
        String ctx = null;
        ArrayList<UploadData> uploadDataList = new ArrayList<UploadData>();
        try {
            offset = jsonObject.getLong("offset");
            size = jsonObject.getInt("size");
            index = jsonObject.getInt("index");
            md5 = jsonObject.optString("md5");
            ctx = jsonObject.optString("ctx");
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

        UploadBlock block = new UploadBlock(offset, size, index, uploadDataList);
        block.md5 = md5;
        block.ctx = ctx;
        return block;
    }

    boolean isCompleted() {
        if (uploadDataList == null) {
            return true;
        }
        boolean isCompleted = true;
        for (UploadData data : uploadDataList) {
            if (!data.isUploaded()) {
                isCompleted = false;
                break;
            }
        }
        return isCompleted;
    }

    long uploadSize() {
        if (uploadDataList == null) {
            return 0;
        }
        long uploadSize = 0;
        for (UploadData data : uploadDataList) {
            uploadSize += data.uploadSize();
        }
        return uploadSize;
    }

    private ArrayList<UploadData> createDataList(int dataSize) {
        long offset = 0;
        int dataIndex = 0;
        ArrayList<UploadData> datas = new ArrayList<UploadData>();
        while (offset < size) {
            long lastSize = size - offset;
            int dataSizeP = Math.min((int) lastSize, dataSize);
            UploadData data = new UploadData(offset, dataSizeP, dataIndex);
            datas.add(data);
            offset += dataSizeP;
            dataIndex += 1;
        }
        return datas;
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("offset", offset);
            jsonObject.putOpt("size", size);
            jsonObject.putOpt("index", index);
            jsonObject.putOpt("md5", md5);
            jsonObject.putOpt("ctx", ctx);
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

    protected UploadData nextUploadDataWithoutCheckData() {
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return null;
        }
        UploadData data = null;
        for (UploadData dataP : uploadDataList) {
            if (dataP.needToUpload()) {
                data = dataP;
                break;
            }
        }
        return data;
    }

    String getUploadContext() {
        return ctx;
    }

    protected void clearUploadState() {
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return;
        }
        for (UploadData data : uploadDataList) {
            data.clearUploadState();
        }
    }

    void updateDataState(UploadData.State state) {
        for (UploadData data : uploadDataList) {
            data.updateState(state);
        }
    }
}
