package com.qiniu.android;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.common.Zone;

/**
 * Created by bailong on 14/10/12.
 */
public final class TestConfig {
    //华东上传凭证
    public static final String bucket_z0 = "sdk-z0";
    public static final String token_z0 = "MP_Ebql_lSsUrDr7WrXn_5vKocQDLvTPCNEFeVmp:zfCfgMtCzO8l8iab_lbm402wZY8=:eyJzY29wZSI6ImFuZHJvaWR0ZXN0IiwiZGVhZGxpbmUiOjE1Njk4NTgzMTl9";
    //华北上传凭证
    public static final String bucket_z1 = "sdk-z1";
    public static final String token_z1 = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm:ILIWwUZ7_hvZeKJzbBKvpo8DpYc=:eyJzY29wZSI6InNkay16MSIsInJldHVybmJvZHkiOiJ7XCJoYXNoXCI6XCIkKGV0YWcpXCIsXCJrZXlcIjpcIiQoa2V5KVwiLFwiZm5hbWVcIjpcIiAkKGZuYW1lKSBcIixcImZzaXplXCI6XCIkKGZzaXplKVwiLFwibWltZVR5cGVcIjpcIiQobWltZVR5cGUpXCIsXCJmb29cIjpcIiQoeDpmb28pXCIsXCJiYXJcIjpcIiQoeDpiYXIpXCJ9IiwiZGVhZGxpbmUiOjQyOTQ5NjcyOTV9";
    //华南上传凭证
    public static final String bucket_z2 = "sdk-z2";
    public static final String token_z2 = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm:CAhJnJxCX7jxibbA5inNym3ol60=:eyJzY29wZSI6InNkay16MiIsInJldHVybmJvZHkiOiJ7XCJoYXNoXCI6XCIkKGV0YWcpXCIsXCJrZXlcIjpcIiQoa2V5KVwiLFwiZm5hbWVcIjpcIiAkKGZuYW1lKSBcIixcImZzaXplXCI6XCIkKGZzaXplKVwiLFwibWltZVR5cGVcIjpcIiQobWltZVR5cGUpXCIsXCJmb29cIjpcIiQoeDpmb28pXCIsXCJiYXJcIjpcIiQoeDpiYXIpXCJ9IiwiZGVhZGxpbmUiOjQyOTQ5NjcyOTV9";
    //北美上传凭证
    public static final String bucket_na0 = "sdk-na0";
    public static final String token_na0 = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm:QxnYkUaYIw4fIsI133_tjFJ_03M=:eyJzY29wZSI6InNkay1uYTAiLCJyZXR1cm5ib2R5Ijoie1wiaGFzaFwiOlwiJChldGFnKVwiLFwia2V5XCI6XCIkKGtleSlcIixcImZuYW1lXCI6XCIgJChmbmFtZSkgXCIsXCJmc2l6ZVwiOlwiJChmc2l6ZSlcIixcIm1pbWVUeXBlXCI6XCIkKG1pbWVUeXBlKVwiLFwiZm9vXCI6XCIkKHg6Zm9vKVwiLFwiYmFyXCI6XCIkKHg6YmFyKVwifSIsImRlYWRsaW5lIjo0Mjk0OTY3Mjk1fQ==";
    public static final String ak = "QWYn5TFQsLLU1pL5MFEmX3s5DmHdUThav9WyOWOm";

    //dns prefetch token
    public static final String uptoken_prefetch = "MP_Ebql_lSsUrDr7WrXn_5vKocQDLvTPCNEFeVmp:3KJpXCGMqm6EAYU71RF1HDmQrcE=:eyJzY29wZSI6ImFuZHJvaWR0ZXN0IiwiZGVhZGxpbmUiOjE1Njc0OTAxODF9";

    //10.31到期
    public static final String uptoken_v3_query ="bjtWBQXrcxgo7HWwlC_bgHg81j352_GhgBGZPeOW:EzHmJLzlO3M7ymBz0xbOfu_pm2c=:eyJzY29wZSI6InNodWFuZ2h1bzEiLCJkZWFkbGluZSI6MTU3MjUzNjQzNX0=";
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
