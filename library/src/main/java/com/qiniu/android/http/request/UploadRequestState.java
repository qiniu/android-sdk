package com.qiniu.android.http.request;

class UploadRequestState {

    private boolean isUseOldServer;
    private boolean isHTTP3;
    private boolean isUserCancel;

    boolean isUserCancel(){
        return isUserCancel;
    }

    void setUserCancel(boolean isUserCancel) {
        this.isUserCancel = isUserCancel;
    }

    public boolean isUseOldServer() {
        return isUseOldServer;
    }

    public void setUseOldServer(boolean useOldServer) {
        isUseOldServer = useOldServer;
    }

    public boolean isHTTP3() {
        return isHTTP3;
    }

    public void setHTTP3(boolean HTTP3) {
        isHTTP3 = HTTP3;
    }
}
