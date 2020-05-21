package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    //华东上传凭证
    public static final String bucket_z0 = "zone0-space";
    public static final String token_z0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:_5feF7zAAx1O-xU9st88yMIfohQ=:eyJzY29wZSI6InpvbmUwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTAxNDU2MDJ9";
    //华北上传凭证
    public static final String bucket_z1 = "zone1-space";
    public static final String token_z1 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:leC4_omECndJD27EcfUWlQ5PRPU=:eyJzY29wZSI6InpvbmUxLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTAxNDU2MDJ9";
    //华南上传凭证
    public static final String bucket_z2 = "zone2-space";
    public static final String token_z2 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:_8knypfwgwNRLpjZlG8B_eO9k3A=:eyJzY29wZSI6InpvbmUyLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTAxNDU2MDJ9";
    //北美上传凭证
    public static final String bucket_na0 = "zone-na0-space";
    public static final String token_na0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:jSVwtVgIm_lvdeZ6xO7KNEHJc0g=:eyJzY29wZSI6InpvbmUtbmEwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTAxNDU2MDJ9";
    public static final String ak = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm";
    //北美上传凭证
    public static final String bucket_as0 = "zone-as0-space";
    public static final String token_as0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:q2bF5q5AKLYhurkGZZdjh-7vgcY=:eyJzY29wZSI6InpvbmUtYXMwLXNwYWNlIiwiZGVhZGxpbmUiOjE1OTAxNDU2MDJ9";

    //dns prefetch token
    public static final String uptoken_prefetch = "MP_Ebql_lSsUrDr7WrXn_5vKocQDLvTPCNEFeVmp:3KJpXCGMqm6EAYU71RF1HDmQrcE=:eyJzY29wZSI6ImFuZHJvaWR0ZXN0IiwiZGVhZGxpbmUiOjE1Njc0OTAxODF9";

    /**
     * 华东机房
     */
    public static final Zone mock_bucket_zone0 = new FixedZone(new String[]{
            "mock.upload.qiniup.com", "mock.upload-nb.qiniup.com",
            "mock.upload-xs.qiniup.com", "up.qiniup.com",
            "up-nb.qiniup.com", "up-xs.qiniup.com",
            "upload.qbox.me", "up.qbox.me"
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
            "mock.upload-fs.qiniup.com", "up-z2.qiniup.com",
            "up-gz.qiniup.com", "up-fs.qiniup.com",
            "upload-z2.qbox.me", "up-z2.qbox.me"
    });

    /**
     * 北美机房
     */
    public static final Zone mock_bucket_zoneNa0 = new FixedZone(new String[]{
            "mock.upload-na0.qiniu.com", "mock.up-na0.qiniup.com",
            "mock.upload-na0.qbox.me", "up-na0.qbox.me"
    });

}
