package com.qiniu.android.http.request;

public class UploadRequestState {

    private boolean isUseOldServer;
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

    protected UploadRequestState clone() {
        UploadRequestState state = new UploadRequestState();
        state.isUseOldServer = isUseOldServer;
        state.isUserCancel = isUserCancel;
        return state;
    }
}
