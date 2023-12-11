package com.qiniu.android.http.request;

import com.qiniu.android.common.ZoneInfo;
import com.qiniu.android.http.ResponseInfo;

/**
 * 上传区域
 *
 * @hidden
 */
public interface IUploadRegion {

    /**
     * 是否有效
     *
     * @return 是否有效
     */
    boolean isValid();

    /**
     * 是否和另一个 region 相等
     *
     * @param region 另一个 region
     * @return 是否和另一个 region 相等
     */
    boolean isEqual(IUploadRegion region);

    /**
     * 获取 ZoneInfo
     *
     * @return ZoneInfo
     */
    ZoneInfo getZoneInfo();

    /**
     * 配置 ZoneInfo
     *
     * @param zoneInfo ZoneInfo
     */
    void setupRegionData(ZoneInfo zoneInfo);

    /**
     * 获取下一个上传的 Server 信息
     *
     * @param requestState 请求状态
     * @param responseInfo 请求响应信息
     * @param freezeServer 冻结的 Server 信息
     * @return 下一个上传的 Server 信息
     */
    IUploadServer getNextServer(UploadRequestState requestState, ResponseInfo responseInfo, IUploadServer freezeServer);

    /**
     * 更新 host 的 IP 缓存信息
     *
     * @param host host
     */
    void updateIpListFormHost(String host);
}
