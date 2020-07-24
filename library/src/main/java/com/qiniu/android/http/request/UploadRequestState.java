package com.qiniu.android.http.request;

class UploadRequestState {

    private boolean isUserCancel;

    boolean isUserCancel(){
        return isUserCancel;
    }

    void setUserCancel(boolean isUserCancel) {
        this.isUserCancel = isUserCancel;
    }
}
