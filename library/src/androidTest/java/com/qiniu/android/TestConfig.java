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
    public static final String token_z0 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:IaPqGd6cum1U9loVq_mw-21KDPg=:eyJzY29wZSI6InNkay16MCIsImRlYWRsaW5lIjoxNjA1NDI3NDYwfQ==";
    //华北上传凭证
    public static final String bucket_z1 = "sdk-z1";
    public static final String token_z1 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:fmLmOMbAj9iu48tKsBTIYtl1dYM=:eyJzY29wZSI6InNkay16MSIsImRlYWRsaW5lIjoxNjA1NDI3NDc3fQ==";
    //华南上传凭证
    public static final String bucket_z2 = "sdk-z2";
    public static final String token_z2 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:cLu5M7XTblvPiRu2qr9PtpDcVaM=:eyJzY29wZSI6InNkay16MiIsImRlYWRsaW5lIjoxNjA1NDI3NDk2fQ==";
    //北美上传凭证
    public static final String bucket_na0 = "sdk-na0";
    public static final String token_na0 = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:oKZwgR14LbCEKXvxJH-9lQLPxZk=:eyJzY29wZSI6InNkay1uYTAiLCJkZWFkbGluZSI6MTYwNTQyNzUxM30=";
    public static final String ak = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW";

    //测试通用的token
    public static final String commonToken = token_na0;

    //dns prefetch token
    public static final String uptoken_prefetch = "bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:WYvuul25V_gUskDyy16eTI9KH_Y=:eyJzY29wZSI6Ind4YXBwdGVzdCIsImRlYWRsaW5lIjoxNTk0MjI0NzYyfQ==";

    /**
     * 华东机房
     */
    public static final Zone mock_bucket_zone0 = new FixedZone(new String[]{
            "upload.qiniup.com", "upload-nb.qiniup.com",
            "upload-xs.qiniup.com", "up.qiniup.com",
            "up-nb.qiniup.com", "up-xs.qiniup.com",
            "upload.qbox.me", "up.qbox.me"
    });

    /**
     * 华北机房
     */
    public static final Zone mock_bucket_zone1 = new FixedZone(new String[]{
            "upload-z1.qiniup.com", "up-z1.qiniup.com",
            "upload-z1.qbox.me", "up-z1.qbox.me"
    });

    /**
     * 华南机房
     */
    public static final Zone mock_bucket_zone2 = new FixedZone(new String[]{
            "up-z2.qiniup.com", "upload-gz.qiniup.com",
            "upload-fs.qiniup.com", "upload-z2.qiniup.com",
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
