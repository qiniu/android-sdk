package com.qiniu.android.http.request;

class UploadRequstState {

    private boolean isUserCancel;

    protected boolean isUserCancel(){
        return isUserCancel;
    }

    protected void setUserCancel(boolean isUserCancel) {
        synchronized (this) {
            this.isUserCancel = isUserCancel;
        }
    }
}
