package com.qiniu.android.common;

import java.net.URI;

/**
 * Created by bailong on 15/10/10.
 */
public abstract class Zone {
    /**
     * 获取上传域名
     */
    protected synchronized String upHost(ZoneInfo zoneInfo, boolean useHttps, String lastUpHost) {
        String upHost = null;
        String upDomain = null;

        if (lastUpHost != null) {
            URI uri = URI.create(lastUpHost);
            //frozen domain
            String frozenDomain = uri.getHost();
            zoneInfo.frozenDomain(frozenDomain);
        }
        //get backup domain
        for (int index = 0; index < zoneInfo.upDomainsList.size(); index++) {
            String domain = zoneInfo.upDomainsList.get(index);
            long frozenTill = zoneInfo.upDomainsMap.get(domain);
            if (frozenTill == 0 || frozenTill <= System.currentTimeMillis() / 1000) {
                upDomain = domain;
                break;
            }
        }

        if (upDomain != null) {
            //reset the selected domain
            zoneInfo.upDomainsMap.put(upDomain, 0L);
        } else {
            //reset the up host frozen time
            for (String domain : zoneInfo.upDomainsList) {
                zoneInfo.upDomainsMap.put(domain, 0L);
            }
            //return the first one as default
            if (zoneInfo.upDomainsList.size() > 0) {
                upDomain = zoneInfo.upDomainsList.get(0);
            }
        }

        if (upDomain != null) {
            if (useHttps) {
                upHost = String.format("https://%s", upDomain);
            } else {
                upHost = String.format("http://%s", upDomain);
            }
        }

        return upHost;
    }

    public abstract String upHost(String upToken, boolean useHttps, String frozenDomain);

    public abstract void frozenDomain(String upHostUrl);

    public abstract void preQuery(String token, QueryHandler complete);

    public abstract boolean preQuery(String token);

    public interface QueryHandler {
        void onSuccess();

        void onFailure(int reason);
    }
}
