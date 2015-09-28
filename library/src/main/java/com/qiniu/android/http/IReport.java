package com.qiniu.android.http;


import cz.msebera.android.httpclient.Header;

public interface IReport {
    Header[] appendStatHeaders(Header[] headers);

    void updateErrorInfo(ResponseInfo info);

    void updateSpeedInfo(ResponseInfo info);
}
