package com.qiniu.android.http.newHttp;

public class UploadRequstState {

    private boolean isUserCancel;

    public boolean getIsUserCancel(){
        return isUserCancel;
    }

    public void setUserCancel(boolean isUserCancel) {
        synchronized (this) {
            this.isUserCancel = isUserCancel;
        }
    }
}
