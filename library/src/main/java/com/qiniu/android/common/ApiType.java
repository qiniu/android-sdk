package com.qiniu.android.common;

public class ApiType {
    public static final int ActionTypeNone = 0;
    public static final int ActionTypeUploadByForm = 1;
    public static final int ActionTypeUploadByResumeV1 = 2;
    public static final int ActionTypeUploadByResumeV2 = 3;

    static String actionTypeString(int actionType) {
        String type = "";
        switch (actionType) {
            case ActionTypeUploadByForm:
                type = "form";
                break;
            case ActionTypeUploadByResumeV1:
                type = "resume-v1";
                break;
            case ActionTypeUploadByResumeV2:
                type = "resume-v2";
                break;
            default:
                break;
        }
        return type;
    }

    static String[] apisWithActionType(int actionType) {
        String[] apis = null;
        switch (actionType) {
            case ActionTypeUploadByForm:
                apis = new String[]{"up.formupload"};
                break;
            case ActionTypeUploadByResumeV1:
                apis = new String[]{"up.mkblk", "up.bput", "up.mkfile"};
                break;
            case ActionTypeUploadByResumeV2:
                apis = new String[]{"up.initparts", "up.uploadpart", "up.completeparts"};
                break;
            default:
                break;
        }
        return apis;
    }
}
