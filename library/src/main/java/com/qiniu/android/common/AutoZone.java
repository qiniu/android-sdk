package com.qiniu.android.common;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by long on 2016/9/29.
 */

public final class AutoZone extends Zone {
    /**
     * 自动判断机房
     */
    public static final AutoZone autoZone = new AutoZone(null);
    private final String ucServer;
    private Map<ZoneIndex, ZoneInfo> zones = new ConcurrentHashMap<>();
    private Client client = new Client();

    public AutoZone(DnsManager dns) {
        this("https://uc.qbox.me", dns);
    }

    AutoZone(String ucServer, DnsManager dns) {
        this.ucServer = ucServer;
    }

    private void getZoneJsonAsync(ZoneIndex index, CompletionHandler handler) {
        String address = ucServer + "/v2/query?ak=" + index.accessKey + "&bucket=" + index.bucket;
        client.asyncGet(address, null, UpToken.NULL, handler);
    }

    private ResponseInfo getZoneJsonSync(ZoneIndex index) {
        String address = ucServer + "/v2/query?ak=" + index.accessKey + "&bucket=" + index.bucket;
        return client.syncGet(address, null);
    }

    // only for test public
    ZoneInfo zoneInfo(String ak, String bucket) {
        ZoneIndex index = new ZoneIndex(ak, bucket);
        return zones.get(index);
    }

    // only for test public
    ZoneInfo queryByToken(String token) {
        try {
            // http://developer.qiniu.com/article/developer/security/upload-token.html
            // http://developer.qiniu.com/article/developer/security/put-policy.html
            String[] strings = token.split(":");
            String ak = strings[0];
            String policy = new String(UrlSafeBase64.decode(strings[2]), Constants.UTF_8);
            JSONObject obj = new JSONObject(policy);
            String scope = obj.getString("scope");
            String bkt = scope.split(":")[0];
            return zoneInfo(ak, bkt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //async
    void preQueryIndex(final ZoneIndex index, final QueryHandler complete) {
        if (index == null) {
            complete.onFailure(ResponseInfo.InvalidToken);
            return;
        }
        ZoneInfo info = zones.get(index);
        if (info != null) {
            complete.onSuccess();
            return;
        }

        getZoneJsonAsync(index, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                if (info.isOK() && response != null) {
                    try {
                        ZoneInfo info2 = ZoneInfo.buildFromJson(response);
                        zones.put(index, info2);
                        complete.onSuccess();
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        complete.onFailure(ResponseInfo.NetworkError);
                        return;
                    }
                }
                complete.onFailure(info.statusCode);
            }
        });
    }

    //sync
    boolean preQueryIndex(final ZoneIndex index) {
        boolean success = false;
        if (index != null) {
            ZoneInfo info = zones.get(index);
            if (info != null) {
                success = true;
            } else {
                try {
                    ResponseInfo responseInfo = getZoneJsonSync(index);
                    ZoneInfo info2 = ZoneInfo.buildFromJson(responseInfo.response);
                    zones.put(index, info2);
                    success = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    @Override
    public synchronized String upHost(String token, boolean useHttps, String frozenDomain) {
        ZoneInfo info = queryByToken(token);
        if (info != null) {
            return super.upHost(info, useHttps, frozenDomain);
        } else {
            return null;
        }
    }

    @Override
    public void preQuery(String token, QueryHandler complete) {
        ZoneIndex index = ZoneIndex.getFromToken(token);
        preQueryIndex(index, complete);
    }

    @Override
    public boolean preQuery(String token) {
        ZoneIndex index = ZoneIndex.getFromToken(token);
        return preQueryIndex(index);
    }

    @Override
    public synchronized void frozenDomain(String upHostUrl) {
        if (upHostUrl != null) {
            URI uri = URI.create(upHostUrl);
            //frozen domain
            String frozenDomain = uri.getHost();
            ZoneInfo zoneInfo = null;
            for (Map.Entry<ZoneIndex, ZoneInfo> entry : this.zones.entrySet()) {
                ZoneInfo eachZoneInfo = entry.getValue();
                if (eachZoneInfo.upDomainsList.contains(frozenDomain)) {
                    zoneInfo = eachZoneInfo;
                    break;
                }
            }
            if (zoneInfo != null) {
                zoneInfo.frozenDomain(frozenDomain);
            }
        }
    }

    static class ZoneIndex {

        final String accessKey;
        final String bucket;

        ZoneIndex(String accessKey, String bucket) {
            this.accessKey = accessKey;
            this.bucket = bucket;
        }

        static ZoneIndex getFromToken(String token) {
            // http://developer.qiniu.com/article/developer/security/upload-token.html
            // http://developer.qiniu.com/article/developer/security/put-policy.html
            String[] strings = token.split(":");
            String ak = strings[0];
            String policy = null;
            try {
                policy = new String(UrlSafeBase64.decode(strings[2]), Constants.UTF_8);
                JSONObject obj = new JSONObject(policy);
                String scope = obj.getString("scope");
                String bkt = scope.split(":")[0];
                return new ZoneIndex(ak, bkt);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public int hashCode() {
            return accessKey.hashCode() * 37 + bucket.hashCode();
        }

        public boolean equals(Object obj) {
            return obj == this || !(obj == null || !(obj instanceof ZoneIndex))
                    && ((ZoneIndex) obj).accessKey.equals(accessKey) && ((ZoneIndex) obj).bucket.equals(bucket);
        }
    }


}
