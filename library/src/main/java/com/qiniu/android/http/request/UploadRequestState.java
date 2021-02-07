package com.qiniu.android.http.request;

public class UploadRequestState {

    private boolean isUseOldServer;
    private boolean isUserCancel;
    private String httpVersion;

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

    public String httpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    protected UploadRequestState clone() {
        UploadRequestState state = new UploadRequestState();
        state.httpVersion = httpVersion;
        state.isUseOldServer = isUseOldServer;
        state.isUserCancel = isUserCancel;
        return state;
    }
}
