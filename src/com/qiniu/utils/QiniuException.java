package com.qiniu.utils;

public class QiniuException extends Exception{
    public final int code;
    public final String reqId;
    public Exception reason = null;
    public QiniuException(int code, String reqId, String desc){
        super(desc);
        this.code = code;
        this.reqId = reqId;
    }
    public QiniuException(int code, String desc, Exception reason){
        super(desc);
        this.code = code;
        this.reason = reason;
        this.reqId = "";
    }

    public static final int Common = -1;
    public static final int IO = -2;
    public static final int JSON = -3;
    public static final int FileNotFound = -4;
    public static final int InvalidEncode = -5;

    public static QiniuException fileNotFound(String desc){
        return new QiniuException(FileNotFound, "", desc);
    }

    public static QiniuException common(String desc, Exception e){
        return new QiniuException(Common, desc, e);
    }
}
