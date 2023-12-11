package com.qiniu.android.http;

/**
 * url 拦截器
 */
public interface UrlConverter {

    /**
     * url 拦截器，可以转换 url
     *
     * @param url url
     * @return 转换后的 url
     */
    String convert(String url);
}
