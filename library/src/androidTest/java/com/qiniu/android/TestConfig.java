package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    // TODO: 2020-05-09 bad token for testPutBytesWithFixedZoneUseBackupDomains
    //华东上传凭证
    public static final String bucket_z0 = "sdk-z0";
    public static final String token_z0 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:fO65e-s-sRrcNiWkZ3qcrCLM3pM=:eyJzY29wZSI6InNkay16MCIsImRlYWRsaW5lIjoxNTk3MjAyMDYwfQ==";
    //华北上传凭证
    public static final String bucket_z1 = "sdk-z1";
    public static final String token_z1 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:KHpcWxGAAut9zJjGaHEqEbjlf-c=:eyJzY29wZSI6InNkay16MSIsImRlYWRsaW5lIjoxNTk3MjAyMDgzfQ==";
    //华南上传凭证
    public static final String bucket_z2 = "sdk-z2";
    public static final String token_z2 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:yt7nub2-FOkK2zwodXSgxinpboE=:eyJzY29wZSI6InNkay16MiIsImRlYWRsaW5lIjoxNTk3MjAyMTAwfQ==";
    //北美上传凭证
    public static final String bucket_na0 = "sdk-na0";
    public static final String token_na0 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:J9AbA4jVl4BtsAFg9XooZaa2Iyk=:eyJzY29wZSI6InNkay1uYTAiLCJkZWFkbGluZSI6MTU5NzIwMjExNX0=";
    
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
