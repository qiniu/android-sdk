package com.qiniu.android.storage;

import com.qiniu.android.utils.MD5;
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

    private final int dataSize;
    private List<UploadData> dataList;

    private boolean isEOF = false;
    private IOException readException = null;

    String uploadId;
    // 单位：秒
    Long expireAt;

    private UploadInfoV2(UploadSource source, int dataSize, List<UploadData> dataList) {
        super(source);
        this.dataSize = dataSize;
        this.dataList = dataList;
    }

    UploadInfoV2(UploadSource source, Configuration configuration) {
        super(source);
        this.dataSize = Math.min(configuration.chunkSize, maxDataSize);
        this.dataList = new ArrayList<>();
    }

    static UploadInfoV2 infoFromJson(UploadSource source, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        int dataSize = 0;
        String type = null;
        Long expireAt = null;
        String uploadId = null;
        List<UploadData> dataList = new ArrayList<>();
        try {
            type = jsonObject.optString(TypeKey);
            dataSize = jsonObject.getInt("dataSize");
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
        } catch (Exception e) {
            return null;
        }

        UploadInfoV2 info = new UploadInfoV2(source, dataSize, dataList);
        info.setInfoFromJson(jsonObject);
        info.expireAt = expireAt;
        info.uploadId = uploadId;

        if (!TypeValue.equals(type) || !source.getId().equals(info.getSourceId())) {
            return null;
        }

        return info;
    }

    int getPartIndexOfData(UploadData data) {
        return data.index + 1; // 片的 index， 从 1 开始
    }

    UploadData nextUploadData() throws IOException {

        // 从 dataList 中读取需要上传的 data
        UploadData data = null;
        while (true) {
            // 从 dataList 中读取需要上传的 data: 未检测数据有效性
            data = nextUploadDataFormDataList();
            if (data == null) {
                break;
            }

            // 加载数据信息并检测数据有效性
            UploadData newData = loadData(data);
            // 根据 data 未加载到数据, data 及其以后的数据是无效的
            if (newData == null) {
                // 有多余的 data 则移除，包含 data
                if (data.size > data.index) {
                    dataList = dataList.subList(0, data.index);
                }
                isEOF = true;
                data = null;
                break;
            }

            // 加载到数据
            // 加载到数据不符合预期，更换 data 信息
            if (newData != data) {
                dataList.set(newData.index, newData);
            }

            // 数据读取结束
            if (newData.size < dataSize) {
                // 有多余的 data 则移除，不包含包含 newData
                if (dataList.size() > newData.index + 1) {
                    dataList = dataList.subList(0, newData.index + 1);
                }
                isEOF = true;
            }

            data = newData;

            if (data.needToUpload()) {
                break;
            }
        }

        if (data != null) {
            return data;
        }

        // 内存的 dataList 中没有可上传的数据，则从资源中读并创建 data
        // 资源读取异常，不可读取
        if (readException != null) {
            throw readException;
        }

        // 资源已经读取完毕，不能再读取
        if (isEOF) {
            return null;
        }

        // 从资源中读取新的 data 进行上传
        long dataOffset = 0;
        if (dataList.size() > 0) {
            UploadData lastData = dataList.get(dataList.size() - 1);
            dataOffset = lastData.offset + lastData.size;
        }
        int dataIndex = dataList.size();
        data = new UploadData(dataOffset, dataSize, dataIndex);
        data = loadData(data);
        // 资源 EOF
        if (data == null || data.size < dataSize) {
            isEOF = true;
        }

        // 读到 data,由于是新数据，则必定为需要上传的数据
        if (data != null) {
            dataList.add(data);
        }

        return data;
    }

    private UploadData nextUploadDataFormDataList() {
        if (dataList == null || dataList.size() == 0) {
            return null;
        }
        UploadData data = null;
        for (UploadData dataP : dataList) {
            if (dataP.needToUpload()) {
                data = dataP;
                break;
            }
        }
        return data;
    }

    // 加载片中的数据
    // 1. 数据片已加载，直接返回
    // 2. 数据块未加载，读块数据
    // 2.1 如果未读到数据，则已 EOF，返回 null
    // 2.2 如果块读到数据
    // 2.2.1 如果块数据符合预期，当片未上传，则加载片数据
    // 2.2.2 如果块数据不符合预期，创建新块，加载片信息
    private UploadData loadData(UploadData data) throws IOException {
        if (data == null) {
            return null;
        }

        // 之前已加载并验证过数据，不必在验证
        if (data.data != null) {
            return data;
        }

        // 根据 data 信息加载 dataBytes
        byte[] dataBytes = null;
        try {
            dataBytes = readData(data.size, data.offset);
        } catch (IOException e) {
            readException = e;
            throw e;
        }

        // 没有数据不需要上传
        if (dataBytes == null || dataBytes.length == 0) {
            return null;
        }

        String md5 = MD5.encrypt(dataBytes);
        // 判断当前 block 的数据是否和实际数据吻合，不吻合则之前 block 被抛弃，重新创建 block
        if (dataBytes.length != data.size || data.md5 == null || !data.md5.equals(md5)) {
            data = new UploadData(data.offset, dataBytes.length, data.index);
            data.md5 = md5;
        }

        if (StringUtils.isNullOrEmpty(data.etag)) {
            data.data = dataBytes;
            data.updateState(UploadData.State.WaitToUpload);
        } else {
            data.updateState(UploadData.State.Complete);
        }

        return data;
    }

    List<Map<String, Object>> getPartInfoArray() {
        if (uploadId == null || uploadId.length() == 0) {
            return null;
        }
        ArrayList<Map<String, Object>> infoArray = new ArrayList<>();
        for (UploadData data : dataList) {
            if (data.getState() == UploadData.State.Complete && !StringUtils.isNullOrEmpty(data.etag)) {
                HashMap<String, Object> info = new HashMap<>();
                info.put("etag", data.etag);
                info.put("partNumber", getPartIndexOfData(data));
                infoArray.add(info);
            }
        }
        return infoArray;
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

    // 文件已经读取结束 & 所有片均上传
    @Override
    boolean isAllUploaded() {
        if (!isEOF) {
            return false;
        }

        if (dataList == null || dataList.size() == 0) {
            return true;
        }
        boolean isCompleted = true;
        for (UploadData data : dataList) {
            if (!data.isUploaded()) {
                isCompleted = false;
                break;
            }
        }
        return isCompleted;
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
        } catch (Exception ignored) {
            return null;
        }
        return jsonObject;
    }
}
