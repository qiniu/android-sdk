package com.qiniu.android.collect;

/**
 * 实现接口，需要log打点时直接send回来处理
 * 后面拓展直接搜LogHandler.send可查询打点位置，新增直接LogHandler.send即可
 */
public interface LogHandler {
    void send(String key, Object value);

    Object getUploadInfo();
}
