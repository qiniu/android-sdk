package com.qiniu.android.utils;

import android.content.Context;
import android.os.Environment;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;

import com.qiniu.android.common.Constants;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;

public class Utils {

    @Contract(pure = true)
    public static String sdkVerion(){
        return Constants.VERSION;
    }

    @NotNull
    @Contract(pure = true)
    public static String sdkLanguage(){
        return "Java";
    }

    @NotNull
    public static Integer getCurrentProcessID(){
        return android.os.Process.myPid();
    }

    @NotNull
    public static Long getCurrentThreadID(){
        Thread thread = Thread.currentThread();
        return thread.getId();
    }

    public static String systemName(){
        return System.getProperty("os.name");
    }

    @Nullable
    @Contract(pure = true)
    public static Integer getCurrentSignalStrength(){
        return null;
    }

    @Nullable
    @Contract(pure = true)
    public static Integer getCurrentNetworkType(){
        return null;
    }

    public static long currentTimestamp(){
        return new Date().getTime();
    }

    @NotNull
    public static  String sdkDirectory(){
        File file = Environment.getDataDirectory();
        String directory = file.getPath() + "/qiniu";
        return directory;
    }
}
