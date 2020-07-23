package com.qiniu.android.http.request;

class UploadRequestState {

    boolean isUserCancel;

    boolean isUserCancel(){
        return isUserCancel;
    }

    void setUserCancel(boolean isUserCancel) {
        synchronized (this) {
            this.isUserCancel = isUserCancel;
        }
    }
}
