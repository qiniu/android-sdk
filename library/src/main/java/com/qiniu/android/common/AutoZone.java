package com.qiniu.android.common;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpToken;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by long on 2016/9/29.
 */

public final class AutoZone extends Zone {
    private static Map<ZoneIndex, ZoneInfo> zones = new ConcurrentHashMap<>();
    private static Client client = new Client();
    private final String ucServer;
    private final DnsManager dns;
    private final boolean https;

    public AutoZone(boolean https, DnsManager dns) {
        this("https://uc.qbox.me", https, dns);
    }

    AutoZone(String ucServer, boolean https, DnsManager dns) {
        this.ucServer = ucServer;
        this.https = https;
        this.dns = dns;
    }

    private void getZoneJsonAsync(ZoneIndex index, CompletionHandler handler) {
        String address = ucServer + "/v1/query?ak=" + index.accessKey + "&bucket=" + index.bucket;
        client.asyncGet(address, null, UpToken.NULL, handler);
    }

    private void putHosts(ZoneInfo info) {
        if (dns != null) {
            try {
                String httpDomain = new URI(info.upHost).getHost();
                String httpsDomain = new URI(info.upHttps).getHost();
                String httpBackDomain = new URI(info.upBackup).getHost();
                dns.putHosts(httpDomain, info.upIp);
                dns.putHosts(httpsDomain, info.upIp);
                dns.putHosts(httpBackDomain, info.upIp);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    // only for test public
    ZoneInfo zoneInfo(String ak, String bucket) {
        ZoneIndex index = new ZoneIndex(ak, bucket);
        ZoneInfo info = zones.get(index);
        return info;
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

    public ServiceAddress upHost(String token) {
        ZoneInfo info = queryByToken(token);
        if (info == null) {
            return null;
        }
        if (https) {
            return new ServiceAddress(info.upHttps);
        }
        return new ServiceAddress(info.upHost, new String[]{info.upIp});
    }

    public ServiceAddress upHostBackup(String token) {
        ZoneInfo info = queryByToken(token);
        if (info == null) {
            return null;
        }
        if (https) {
            return null;
        }
        return new ServiceAddress(info.upBackup, new String[]{info.upIp});
    }

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
                        putHosts(info2);
                        complete.onSuccess();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        complete.onFailure(ResponseInfo.NetworkError);
                    }
                }
            }
        });
    }

    @Override
    public void preQuery(String token, QueryHandler complete) {
        ZoneIndex index = ZoneIndex.getFromToken(token);
        preQueryIndex(index, complete);
    }

    static class ZoneInfo {
        final String upHost;
        final String upIp;
        final String upBackup;
        final String upHttps;

        private ZoneInfo(String upHost, String upIp, String upBackup, String upHttps) {
            this.upHost = upHost;
            this.upIp = upIp;
            this.upBackup = upBackup;
            this.upHttps = upHttps;
        }

        static ZoneInfo buildFromJson(JSONObject obj) throws JSONException {
            JSONObject http = obj.getJSONObject("http");
            JSONArray up = http.getJSONArray("up");
            String upHost = up.getString(1);
            String upBackup = up.getString(0);
            String upIp = up.getString(2).split(" ")[2].split("//")[1];
            JSONObject https = obj.getJSONObject("https");
            String upHttps = https.getJSONArray("up").getString(0);
            return new ZoneInfo(upHost, upIp, upBackup, upHttps);
        }
    }

    static class ZoneIndex {
        private final String accessKey;
        private final String bucket;

        ZoneIndex(String accessKey, String bucket) {
            this.accessKey = accessKey;
            this.bucket = bucket;
        }

        public static ZoneIndex getFromToken(String token) {
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
