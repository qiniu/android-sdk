package com.qiniu.android.utils;


import com.qiniu.android.common.Constants;


import java.util.Date;

public class Utils {

    public static String sdkVerion(){
        return Constants.VERSION;
    }

    public static String sdkLanguage(){
        return "Java";
    }

    public static Integer getCurrentProcessID(){
        return android.os.Process.myPid();
    }

    public static Long getCurrentThreadID(){
        Thread thread = Thread.currentThread();
        return thread.getId();
    }

    public static String systemName(){
        return System.getProperty("os.name");
    }

    public static String systemVersion(){
        return System.getProperty("os.version");
    }

    public static Integer getCurrentSignalStrength(){
        return null;
    }

    public static Integer getCurrentNetworkType(){
        return null;
    }

    public static long currentTimestamp(){
        return new Date().getTime();
    }

    public static  String sdkDirectory(){
        String directory = ContextGetter.applicationContext().getCacheDir().getAbsolutePath() + "/qiniu";
        return directory;
    }
}
