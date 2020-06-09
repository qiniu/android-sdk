package com.qiniu.android.utils;

import android.util.Log;

/**
 * Created by yangsen on 2020/6/4
 */
public class LogUtil {

    private static boolean showLog = true;
    private static int showLevel = Log.VERBOSE;

    /**
     * Send a VERBOSE log message.
     * @param msg The message you would like logged.
     */
    public static int v(String msg) {
        return println(Log.VERBOSE, null, msg, null);
    }

    /**
     * Send a VERBOSE log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        return println(Log.VERBOSE, tag, msg, null);
    }

    /**
     * Send a VERBOSElog message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        return println(Log.VERBOSE, tag, msg, tr);
    }

    /**
     * Send a DEBUG log message.
     * @param msg The message you would like logged.
     */
    public static int d(String msg) {
        return println(Log.DEBUG, null, msg, null);
    }

    /**
     * Send a DEBUG log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        return println(Log.DEBUG, tag, msg, null);
    }

    /**
     * Send a DEBUG log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        return println(Log.DEBUG, tag, msg, tr);
    }

    /**
     * Send an INFO log message.
     * @param msg The message you would like logged.
     */
    public static int i(String msg) {
        return println(Log.INFO, null, msg, null);
    }

    /**
     * Send an INFO log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        return println(Log.INFO, tag, msg, null);
    }

    /**
     * Send a INFO log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        return println(Log.INFO, tag, msg, tr);
    }

    /**
     * Send a WARN log message.
     * @param msg The message you would like logged.
     */
    public static int w(String msg) {
        return println(Log.WARN, null, msg, null);
    }

    /**
     * Send a WARN log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        return println(Log.WARN, tag, msg, null);
    }

    /**
     * Send a WARN log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        return println(Log.WARN, tag, msg, tr);
    }

    /*
     * Send a WARN log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        return println(Log.WARN, tag, null, tr);
    }

    /**
     * Send an ERROR log message.
     * @param msg The message you would like logged.
     */
    public static int e(String msg) {
        return println(Log.ERROR, null, msg, null);
    }

    /**
     * Send an ERROR log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        return println(Log.ERROR, tag, msg, null);
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        return println(Log.ERROR, tag, msg, tr);
    }

    private static int println(int logLevel, String tag, String msg, Throwable tr) {
        if (shouldLog(logLevel, tag, msg, tr)){
            switch (logLevel) {
                case Log.VERBOSE: {
                    if (tr == null){
                        return Log.v(recreateLogTag(tag), recreateLogMessage(msg));
                    } else {
                        return Log.v(recreateLogTag(tag), recreateLogMessage(msg), tr);
                    }
                }
                case Log.DEBUG: {
                    if (tr == null){
                        return Log.d(recreateLogTag(tag), recreateLogMessage(msg));
                    } else {
                        return Log.d(recreateLogTag(tag), recreateLogMessage(msg), tr);
                    }
                }
                case Log.INFO: {
                    if (tr == null){
                        return Log.i(recreateLogTag(tag), recreateLogMessage(msg));
                    } else {
                        return Log.i(recreateLogTag(tag), recreateLogMessage(msg), tr);
                    }
                }
                case Log.WARN: {
                    if (tr == null){
                        return Log.w(recreateLogTag(tag), recreateLogMessage(msg));
                    } else {
                        return Log.w(recreateLogTag(tag), recreateLogMessage(msg), tr);
                    }
                }
                case Log.ERROR: {
                    if (tr == null){
                        return Log.e(recreateLogTag(tag), recreateLogMessage(msg));
                    } else {
                        return Log.e(recreateLogTag(tag), recreateLogMessage(msg), tr);
                    }
                }
                default:{
                    return -1;
                }
            }
        } else {
            return -1;
        }
    }

    private static boolean shouldLog(int logLevel, String tag, String msg, Throwable tr){
        if (logLevel < showLevel || (msg == null || msg.length() == 0) && tr == null){
            return false;
        } else {
            return showLog;
        }
    }

    private static String recreateLogTag(String tag){
        String tagp = tag != null ? (tag+":") : "";
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length > 5 && elements[5] != null){
            String fileName = elements[5].getFileName() != null ? elements[5].getFileName() : "?";
            int lineNumber = elements[5].getLineNumber();
            return "[QiNiu:" + tagp + fileName + ":" + lineNumber + "]";
        } else {
            return "[QiNiu:]" + tagp + "]";
        }
    }

    private static String recreateLogMessage(String msg){
        return (msg != null ? msg : "");
    }
}
