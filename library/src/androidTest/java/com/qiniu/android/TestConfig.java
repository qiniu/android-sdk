package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    // TODO: 2020-05-09 bad token for testPutBytesWithFixedZoneUseBackupDomains
// 华东上传凭证
    public static final String bucket_z0 = "zone0-space";
    public static final String token_z0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:DlmsWHL94KlobUmqDwNIED_PXto=:eyJzY29wZSI6InpvbmUwLXNwYWNlIiwiZGVhZGxpbmUiOjE2MDE5NjU4MTEsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
    // 华北上传凭证
    public static final String bucket_z1 = "zone1-space";
    public static final String token_z1 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:WyVx1hNwdJX_YtQEfToV3P0aUmk=:eyJzY29wZSI6InpvbmUxLXNwYWNlIiwiZGVhZGxpbmUiOjE2MDE5NjU4MTEsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
    // 华南上传凭证
    public static final String bucket_z2 = "zone2-space";
    public static final String token_z2 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:OyuH1MxMGOEc8DJ-JeKO3SEtUqw=:eyJzY29wZSI6InpvbmUyLXNwYWNlIiwiZGVhZGxpbmUiOjE2MDE5NjU4MTEsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
    // 北美上传凭证
    public static final String bucket_na0 = "zone-na0-space";
    public static final String token_na0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:h0eY-du2nDGj-z4ys9qQKRjn4IQ=:eyJzY29wZSI6InpvbmUtbmEwLXNwYWNlIiwiZGVhZGxpbmUiOjE2MDE5NjU4MTEsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
    // 东南亚上传凭证
    public static final String bucket_as0 = "zone-as0-space";
    public static final String token_as0 = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:BJmcNL_9ymdTEkP9FQqbYsOVY_M=:eyJzY29wZSI6InpvbmUtYXMwLXNwYWNlIiwiZGVhZGxpbmUiOjE2MDE5NjU4MTEsICJyZXR1cm5Cb2R5Ijoie1wiZm9vXCI6JCh4OmZvbyksIFwiYmFyXCI6JCh4OmJhciksIFwibWltZVR5cGVcIjokKG1pbWVUeXBlKSwgXCJoYXNoXCI6JChldGFnKSwgXCJrZXlcIjokKGtleSksIFwiZm5hbWVcIjokKGZuYW1lKX0ifQ==";
    public static final String invalidBucketToken = "jH983zIUFIP1OVumiBVGeAfiLYJvwrF45S-t22eu:ZZJgEHxd4sCVSyv5fWzQuxEKO8A=:eyJzY29wZSI6InpvbmVfaW52YWxpZCIsImRlYWRsaW5lIjoxNjAxOTY1ODExLCAicmV0dXJuQm9keSI6IntcImZvb1wiOiQoeDpmb28pLCBcImJhclwiOiQoeDpiYXIpLCBcIm1pbWVUeXBlXCI6JChtaW1lVHlwZSksIFwiaGFzaFwiOiQoZXRhZyksIFwia2V5XCI6JChrZXkpLCBcImZuYW1lXCI6JChmbmFtZSl9In0=";

    // -----------
    public static final String ak = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW";


    //测试通用的token
    public static final String commonToken = token_na0;
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
