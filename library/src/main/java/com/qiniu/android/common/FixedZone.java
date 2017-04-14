package com.qiniu.android.common;

/**
 * Created by long on 2016/9/29.
 */

public final class FixedZone extends Zone {
    private final ServiceAddress up;
    private final ServiceAddress upBackup;

    public FixedZone(ServiceAddress up, ServiceAddress upBackup) {
        this.up = up;
        this.upBackup = upBackup;
    }

    public ServiceAddress upHost(String token) {
        return up;
    }

    public ServiceAddress upHostBackup(String token) {
        return upBackup;
    }

    @Override
    public void preQuery(String token, QueryHandler complete) {
        complete.onSuccess();
    }
}
