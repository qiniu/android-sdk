package com.qiniu.android.http.request;

public class UploadRequstState {

    private boolean isUserCancel;

    public boolean isUserCancel(){
        return isUserCancel;
    }

    public void setUserCancel(boolean isUserCancel) {
        synchronized (this) {
            this.isUserCancel = isUserCancel;
        }
    }
}
