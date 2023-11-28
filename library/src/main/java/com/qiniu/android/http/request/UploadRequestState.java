package com.qiniu.android.http.request;

/**
 * 上传状态
 */
public class UploadRequestState {

    private boolean couldUseHttp3;
    private boolean isUseOldServer;
    private boolean isUserCancel;

    /**
     * 构造函数
     */
    public UploadRequestState() {
    }

    /**
     * 是否可以使用 HTTP/3
     *
     * @return 是否可以使用 HTTP/3
     */
    public boolean couldUseHttp3() {
        return couldUseHttp3;
    }

    void setCouldUseHttp3(boolean couldUseHttp3) {
        this.couldUseHttp3 = couldUseHttp3;
    }

    boolean isUserCancel() {
        return isUserCancel;
    }

    void setUserCancel(boolean isUserCancel) {
        this.isUserCancel = isUserCancel;
    }

    /**
     * 是否使用支持 sni 的域名
     *
     * @return 是否使用支持 sni 的域名
     */
    public boolean isUseOldServer() {
        return isUseOldServer;
    }

    /**
     * 设置是否使用支持 sni 的域名
     *
     * @param useOldServer 是否使用支持 sni 的域名
     */
    public void setUseOldServer(boolean useOldServer) {
        isUseOldServer = useOldServer;
    }

    /**
     * clone
     *
     * @return clone 后的对象
     */
    protected UploadRequestState clone() {
        UploadRequestState state = new UploadRequestState();
        state.isUseOldServer = isUseOldServer;
        state.isUserCancel = isUserCancel;
        return state;
    }
}
