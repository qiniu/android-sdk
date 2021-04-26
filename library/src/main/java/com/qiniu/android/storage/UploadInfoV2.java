package com.qiniu.android.storage;

import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UploadInfoV2 extends UploadInfo {
    private final static String TypeKey = "infoType";
    private final static String TypeValue = "UploadInfoV2";
    private final static int maxDataSize = 1024 * 1024 * 1024;

    private int dataSize;
    private boolean isEOF = false;
    private List<UploadData> dataList;

    String uploadId;
    // 单位：秒
    Long expireAt;

    private UploadInfoV2() {
    }

    UploadInfoV2(UploadSource source, Configuration configuration) {
        super(source, configuration);

        if (configuration.chunkSize > maxDataSize) {
            this.dataSize = maxDataSize;
        } else {
            this.dataSize = configuration.chunkSize;
        }

        dataList = createDataList(this.dataSize);
    }

    static UploadInfoV2 infoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        long sourceSize = 0;
        int dataSize = 0;
        String type = null;
        String sourceId = null;
        Long expireAt = null;
        String uploadId = null;
        List<UploadData> dataList = new ArrayList<>();
        try {
            type = jsonObject.optString(TypeKey);
            sourceSize = jsonObject.getLong("sourceSize");
            dataSize = jsonObject.getInt("dataSize");
            sourceId = jsonObject.optString("sourceId");
            expireAt = jsonObject.getLong("expireAt");
            uploadId = jsonObject.optString("uploadId");
            JSONArray dataJsonArray = jsonObject.getJSONArray("dataList");
            for (int i = 0; i < dataJsonArray.length(); i++) {
                JSONObject dataJson = dataJsonArray.getJSONObject(i);
                UploadData data = UploadData.dataFromJson(dataJson);
                if (data != null) {
                    dataList.add(data);
                }
            }
        } catch (JSONException e) {
        }

        if (!TypeValue.equals(type)) {
            return null;
        }

        UploadInfoV2 info = new UploadInfoV2();
        info.sourceSize = sourceSize;
        info.dataSize = dataSize;
        info.dataList = dataList;
        info.sourceId = sourceId;
        info.expireAt = expireAt;
        info.uploadId = uploadId;
        info.setSource(source);
        return info;
    }

    UploadData nextUploadData() throws IOException {
        // 1. 从内存的 dataList 中读取需要上传的 block
        UploadData data = nextUploadDataFormDataList();
        if (data != null) {
            if (data.data == null) {
                data.data = readData(data.size, data.offset);
            }
            return data;
        }

        // 2. 资源已经读取完毕，不能再读取
        if (isEOF) {
            return null;
        }

        // 3. 从资源中读取新的 data 进行上传
        long dataOffset = 0;

        if (dataList.size() > 0) {
            UploadData lastData = dataList.get(dataList.size() - 1);
            dataOffset = lastData.offset + lastData.size;
        }

        int dataIndex = dataList.size() + 1; // 片的 index， 从 1 开始
        int dataSize = this.dataSize; // 片的大小
        // 读取片数据
        byte[] dataBytes = readData(dataSize, dataOffset);

        // 片数据大小不符合预期说明已经读到文件结尾
        if (dataBytes.length < dataSize) {
            dataSize = dataBytes.length;
            isEOF = true;
        }

        // 未读到数据不必构建片模型
        if (dataSize == 0) {
            return null;
        }

        // 构造片模型
        data = new UploadData(dataOffset, dataSize, dataIndex);
        data.data = dataBytes;
        dataList.add(data);

        return data;
    }

    private UploadData nextUploadDataFormDataList() {
        if (dataList == null || dataList.size() == 0) {
            return null;
        }
        UploadData data = null;
        for (UploadData dataP : dataList) {
            if (!dataP.isCompleted && !dataP.isUploading) {
                data = dataP;
                break;
            }
        }
        return data;
    }

    List<Map<String, Object>> getPartInfoArray() {
        if (uploadId == null || uploadId.length() == 0) {
            return null;
        }
        ArrayList<Map<String, Object>> infoArray = new ArrayList<>();
        for (UploadData data : dataList) {
            if (data.etag != null) {
                HashMap<String, Object> info = new HashMap<>();
                info.put("etag", data.etag);
                info.put("partNumber", data.index);
                infoArray.add(info);
            }
        }
        return infoArray;
    }

    @Override
    boolean isSameUploadInfo(UploadInfo info) {
        if (!super.isSameUploadInfo(info)) {
            return false;
        }

        if (!(info instanceof UploadInfoV2)) {
            return false;
        }

        UploadInfoV2 infoV2 = (UploadInfoV2) info;
        return dataSize == infoV2.dataSize;
    }

    @Override
    void clearUploadState() {
        for (UploadData data : dataList) {
            data.clearUploadState();
        }
    }

    @Override
    long uploadSize() {
        if (dataList == null || dataList.size() == 0) {
            return 0;
        }
        long uploadSize = 0;
        for (UploadData data : dataList) {
            uploadSize += data.uploadSize();
        }
        return uploadSize;
    }

    @Override
    boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        if (StringUtils.isNullOrEmpty(uploadId) || expireAt == null) {
            return false;
        }

        long timestamp = new Date().getTime() / 1000;
        return expireAt > (timestamp - 3600 * 24 * 2);
    }

    @Override
    boolean isAllUploadingOrUploaded() {
        return false;
    }

    @Override
    boolean isAllUploaded() {
        if (getSourceSize() <= 0) {
            return false;
        }

        if (dataList == null || dataList.size() == 0) {
            return true;
        }
        boolean isCompleted = true;
        for (UploadData data : dataList) {
            if (!data.isCompleted) {
                isCompleted = false;
                break;
            }
        }
        return isCompleted;
    }

    @Override
    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(TypeKey, TypeValue);
            jsonObject.put("sourceId", sourceId);
            jsonObject.put("sourceSize", sourceSize);
            jsonObject.put("dataSize", dataSize);
            jsonObject.put("expireAt", expireAt);
            jsonObject.put("uploadId", uploadId);
            if (dataList != null && dataList.size() > 0) {
                JSONArray dataJsonArray = new JSONArray();
                for (UploadData data : dataList) {
                    JSONObject dataJson = data.toJsonObject();
                    if (dataJson != null) {
                        dataJsonArray.put(dataJson);
                    }
                }
                jsonObject.put("dataList", dataJsonArray);
            }
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private List<UploadData> createDataList(int dataSize) {
        List<UploadData> dataList = new ArrayList<UploadData>();
        if (sourceSize <= UploadSource.UnknownSourceSize) {
            return dataList;
        }

        long offset = 0;
        int dataIndex = 1;
        while (offset < sourceSize) {
            long lastSize = sourceSize - offset;
            int dataSizeP = Math.min((int) lastSize, dataSize);
            UploadData data = new UploadData(offset, dataSizeP, dataIndex);
            dataList.add(data);
            offset += dataSizeP;
            dataIndex += 1;
        }
        return dataList;
    }
}
