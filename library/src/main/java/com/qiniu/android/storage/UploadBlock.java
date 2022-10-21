package com.qiniu.android.storage;

import com.qiniu.android.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class UploadBlock {

    final long offset;
    final int size;
    final int index;
    final List<UploadData> uploadDataList;

    // 单位：秒
    Long expireAt = null;

    String md5 = null;
    String ctx = null;

    UploadBlock(long offset, int blockSize, int dataSize, int index) {
        this.offset = offset;
        this.size = blockSize;
        this.index = index;
        this.uploadDataList = createDataList(dataSize);
    }

    private UploadBlock(long offset, int blockSize, int index, List<UploadData> uploadDataList) {
        this.offset = offset;
        this.size = blockSize;
        this.index = index;
        this.uploadDataList = uploadDataList;
    }

    static UploadBlock blockFromJson(JSONObject jsonObject) throws Exception {
        if (jsonObject == null) {
            return null;
        }

        long offset = jsonObject.getLong("offset");
        int size = jsonObject.getInt("size");
        int index = jsonObject.getInt("index");
        long expireAt = jsonObject.getLong("expired_at");
        String md5 = jsonObject.optString("md5");
        String ctx = jsonObject.optString("ctx");
        ArrayList<UploadData> uploadDataList = new ArrayList<UploadData>();
        JSONArray dataJsonArray = jsonObject.getJSONArray("uploadDataList");
        for (int i = 0; i < dataJsonArray.length(); i++) {
            JSONObject dataJson = dataJsonArray.getJSONObject(i);
            UploadData data = UploadData.dataFromJson(dataJson);
            if (data != null) {
                uploadDataList.add(data);
            }
        }

        UploadBlock block = new UploadBlock(offset, size, index, uploadDataList);
        block.expireAt = expireAt;
        block.md5 = md5;
        block.ctx = ctx;
        return block;
    }

    boolean isValid() {
        if (expireAt == null || expireAt == 0) {
            // 不存在时，为新创建 block: 有效
            return true;
        }

        // 存在则有效期必须为过期
        return (expireAt - 2 * 3600) > Utils.currentSecondTimestamp();
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

    void checkInfoStateAndUpdate() {
        for (UploadData data : uploadDataList) {
            data.checkStateAndUpdate();
        }
    }

    JSONObject toJsonObject() throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("offset", offset);
        jsonObject.putOpt("size", size);
        jsonObject.putOpt("index", index);
        jsonObject.putOpt("expired_at", expireAt);
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

    protected void clearUploadState() {
        md5 = null;
        ctx = null;
        if (uploadDataList == null || uploadDataList.size() == 0) {
            return;
        }
        for (UploadData data : uploadDataList) {
            data.clearUploadState();
        }
    }
}
