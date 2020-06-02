package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    // 华东上传凭证
    public static final String bucket_z0 = "zone0-space";
    public static final String token_z0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:VZrgyiQrF46-YVz7ScjhmaM_f5M=:eyJzY29wZSI6InpvbmUwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTExNTI4NDcsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSl9In0=";
    // 华北上传凭证
    public static final String bucket_z1 = "zone1-space";
    public static final String token_z1 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:bHXOvkBzr7_RnLba2pxqnpyl4Z4=:eyJzY29wZSI6InpvbmUxLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTExNTI4NDcsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSl9In0=";
    // 华南上传凭证
    public static final String bucket_z2 = "zone2-space";
    public static final String token_z2 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:z0SH26O_m5bWoK6GS2V8CjeG8zo=:eyJzY29wZSI6InpvbmUyLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTExNTI4NDcsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSl9In0=";
    // 北美上传凭证
    public static final String bucket_na0 = "zone-na0-space";
    public static final String token_na0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:8K22UcPmv-Z_0lekcKGuV1xPclQ=:eyJzY29wZSI6InpvbmUtbmEwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTExNTI4NDcsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSl9In0=";
    // 东南亚上传凭证
    public static final String bucket_as0 = "zone-as0-space";
    public static final String token_as0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:0mWm4fhiQnXIUOIkbo9-6myeuaA=:eyJzY29wZSI6InpvbmUtYXMwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTExNTI4NDcsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSl9In0=";



    // -----------
    public static final String ak = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm";
    //dns prefetch token
    public static final String uptoken_prefetch = "MP_Ebql_lSsUrDr7WrXn_5vKocQDLvTPCNEFeVmp:3KJpXCGMqm6EAYU71RF1HDmQrcE=:eyJzY29wZSI6ImFuZHJvaWR0ZXN0IiwiZGVhZGxpbmUiOjE1Njc0OTAxODF9";

    /**
     * 华东机房
     */
    public static final Zone mock_bucket_zone0 = new FixedZone(new String[]{
            "mock.upload.qiniup.com", "mock.upload-nb.qiniup.com",
            "mock.upload-xs.qiniup.com", "mock.up.qiniup.com",
            "mock.up-nb.qiniup.com", "mock.up-xs.qiniup.com",
            "mock.upload.qbox.me", "up.qbox.me"
    });

    /**
     * 华北机房
     */
    public static final Zone mock_bucket_zone1 = new FixedZone(new String[]{
            "mock.upload-z1.qiniup.com", "mock.up-z1.qiniup.com",
            "mock.upload-z1.qbox.me", "up-z1.qbox.me"
    });

    /**
     * 华南机房
     */
    public static final Zone mock_bucket_zone2 = new FixedZone(new String[]{
            "mock.upload-z2.qiniup.com", "mock.upload-gz.qiniup.com",
            "mock.upload-fs.qiniup.com", "mock.up-z2.qiniup.com",
            "mock.up-gz.qiniup.com", "mock.up-fs.qiniup.com",
            "mock.upload-z2.qbox.me", "up-z2.qbox.me"
    });

    /**
     * 北美机房
     */
    public static final Zone mock_bucket_zoneNa0 = new FixedZone(new String[]{
            "mock.upload-na0.qiniu.com", "mock.up-na0.qiniup.com",
            "mock.upload-na0.qbox.me", "up-na0.qbox.me"
    });

}
