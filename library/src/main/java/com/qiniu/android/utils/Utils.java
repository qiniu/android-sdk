package com.qiniu.android.utils;


import com.qiniu.android.common.Constants;


import java.util.Arrays;
import java.util.Date;

public class Utils {

    public static String sdkVerion(){
        return Constants.VERSION;
    }

    public static String sdkLanguage(){
        return "Android";
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

    public static String formEscape(String string){
        if (string == null){
            return null;
        }
        String ret = string;
        ret = ret.replace("\\", "\\\\");
        ret = ret.replace("\"", "\\\"");
        return ret;
    }

    public static String getIpType(String ip, String host){
        String type = null;
        if (ip == null || ip.length() == 0) {
            return type;
        }
        if (ip.contains(":")) {
            type = getIPV6StringType(ip, host);
        } else if (ip.contains(".")){
            type = getIPV4StringType(ip, host);
        }
        return type;
    }

    private static String getIPV4StringType(String ipv4String, String host){
        if (host == null){
            host = "";
        }

        String type = null;
        String[] ipNumberStrings = ipv4String.split("\\.");
        if (ipNumberStrings.length == 4){
            int firstNumber = Integer.parseInt(ipNumberStrings[0]);
            if (firstNumber > 0 && firstNumber < 127) {
                type = "ipv4-A-" + firstNumber;
            } else if (firstNumber > 127 && firstNumber <= 191) {
                type = "ipv4-B-" + firstNumber + ipNumberStrings[1];
            } else if (firstNumber > 191 && firstNumber <= 223) {
                type = "ipv4-C-"+ firstNumber + ipNumberStrings[1] + ipNumberStrings[2];
            }
        }
        type = host + "-" + type;
        return type;
    }

    private static String getIPV6StringType(String ipv6String, String host){
        if (host == null){
            host = "";
        }

        String[] ipNumberStrings = ipv6String.split(":");
        String[] ipNumberStringsReal = new String[]{"0000", "0000", "0000", "0000", "0000", "0000", "0000", "0000"};
        String[] suppleStrings = new String[]{"0000", "000", "00", "0", ""};
        int i = 0;
        while (i < ipNumberStrings.length){
            String ipNumberString = ipNumberStrings[i];
            if (ipNumberString.length() > 0){
                ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                ipNumberStringsReal[i] = ipNumberString;
            } else {
                break;
            }
            i++;
        }

        int j = ipNumberStrings.length - 1;
        int indexReal = ipNumberStringsReal.length - 1;
        while (i < j){
            String ipNumberString = ipNumberStrings[j];
            if (ipNumberString.length() > 0){
                ipNumberString = suppleStrings[ipNumberString.length()] + ipNumberString;
                ipNumberStringsReal[indexReal] = ipNumberString;
            } else {
                break;
            }
            j--;
            indexReal--;
        }
        String[] typeNumberArray = Arrays.copyOfRange(ipNumberStringsReal, 0, 4);
        String numberInfo = StringUtils.join(typeNumberArray, "-");
        return host + "-ipv6-" + numberInfo;
    }
}
