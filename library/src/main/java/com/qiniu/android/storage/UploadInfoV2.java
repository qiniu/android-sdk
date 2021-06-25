package com.qiniu.android.storage;

import com.qiniu.android.utils.MD5;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONArray;
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

        UploadData data = nextUploadDataFormDataList();

        // 内存的 blockList 中没有可上传的数据，则从资源中读并创建 block
        if (data == null) {
            if (isEOF) {
                return null;
            } else if (readException != null) {
                // 资源读取异常，不可读取
                throw readException;
            }

            // 从资源中读取新的 data 进行上传
            long dataOffset = 0;
            if (dataList.size() > 0) {
                UploadData lastData = dataList.get(dataList.size() - 1);
                dataOffset = lastData.offset + lastData.size;
            }
            int dataIndex = dataList.size();
            data = new UploadData(dataOffset, dataSize, dataIndex);
        }

        UploadData loadData = null;
        try {
            loadData = loadData(data);
        } catch (IOException e) {
            readException = e;
            throw e;
        }

        synchronized (this) {
            if (loadData == null) {
                // 没有加在到 data, 也即数据源读取结束
                isEOF = true;
                // 有多余的 data 则移除，移除中包含 data
                if (dataList.size() > data.index) {
                    dataList = dataList.subList(0, data.index);
                }
            } else {
                // 加在到 data
                if (loadData.index == dataList.size()) {
                    // 新块：data index 等于 dataList size 则为新创建 block，需要加入 dataList
                    dataList.add(loadData);
                } else if (loadData != data) {
                    // 更换块：重新加在了 data， 更换信息
                    dataList.set(loadData.index, loadData);
                }

                // 数据源读取结束，块读取大小小于预期，读取结束
                if (loadData.size < data.size) {
                    isEOF = true;
                    // 有多余的 block 则移除，移除中不包含 block
                    if (dataList.size() > data.index + 1) {
                        dataList = dataList.subList(0, data.index + 1);
                    }
                }
            }
        }
        return loadData;
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
    synchronized long uploadSize() {
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
    void checkInfoStateAndUpdate() {
        for (UploadData data : dataList) {
            data.checkStateAndUpdate();
        }
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
