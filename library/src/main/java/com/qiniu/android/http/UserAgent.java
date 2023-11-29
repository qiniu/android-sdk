package com.qiniu.android.http;

import android.os.Build;
import android.text.TextUtils;

import com.qiniu.android.common.Constants;
import com.qiniu.android.utils.StringUtils;
import com.qiniu.android.utils.Utils;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Random;

import static java.lang.String.format;

/**
 * Created by bailong on 15/6/23.
 */
public final class UserAgent {
    private static UserAgent _instance = new UserAgent();

    /**
     * id
     */
    public final String id;

    /**
     * UserAgent
     */
    public final String ua;

    private UserAgent() {
        id = genId();
        ua = getUserAgent(id);
    }

    /**
     * 获取 UserAgent 单例
     *
     * @return UserAgent 单例
     */
    public static UserAgent instance() {
        return _instance;
    }

    private static String genId() {
        Random r = new Random();
        return System.currentTimeMillis() + "" + r.nextInt(999);
    }

    static String getUserAgent(String id) {
        String addition = Utils.isDebug() ? "_Debug" : "";
        return format("QiniuAndroid%s/%s (%s; %s; %s", addition, Constants.VERSION,
                Utils.systemVersion(), Utils.systemName(), id);
    }

    /**
     * 获取 UserAgent 字符串
     *
     * @param part part
     * @return UserAgent 字符串
     */
    public String getUa(String part) {
        String _part = ("" + part).trim();
        if (_part.length() > 15) {
            _part = _part.substring(0, Math.min(16, _part.length()));
        }
        return new String((ua + "; " + _part + ")").getBytes(Charset.forName("ISO-8859-1")));
    }

}
