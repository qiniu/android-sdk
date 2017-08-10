package com.qiniu.android.bigdata.pipeline;

import com.qiniu.android.bigdata.Configuration;
import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.StringMap;
import com.qiniu.android.utils.StringUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by long on 2017/7/25.
 */

public final class Pipeline {
    private static final String HTTPHeaderAuthorization = "Authorization";
    private static final String TEXT_PLAIN = "text/plain";
    private final Configuration config;
    private final Client client;

    public Pipeline(Configuration config) {
        this.config = Configuration.copy(config);
        this.client = new Client(this.config.proxy, this.config.connectTimeout, this.config.responseTimeout, null, null);
    }

    public <V> void pump(String repo, Map<String, V> data, String token, PumpCompleteHandler handler) {
        StringBuilder b = new StringBuilder();
        Points.formatPoint(data, b);
        send(repo, b, token, handler);
    }

    public void pump(String repo, Object data, String token, PumpCompleteHandler handler) {
        StringBuilder b = new StringBuilder();
        Points.formatPoint(data, b);
        send(repo, b, token, handler);
    }

    public <V> void pumpMulti(String repo, Map<String, V>[] data, String token, PumpCompleteHandler handler) {
        StringBuilder b = Points.formatPoints(data);
        send(repo, b, token, handler);
    }

    public void pumpMultiObjects(String repo, Object[] data, String token, PumpCompleteHandler handler) {
        StringBuilder b = Points.formatPoints(data);
        send(repo, b, token, handler);
    }

    public <V> void pumpMultiObjects(String repo, List<V> data, String token, PumpCompleteHandler handler) {
        StringBuilder b = Points.formatPointsObjects(data);
        send(repo, b, token, handler);
    }

    public <V> void pumpMulti(String repo, List<Map<String, V>> data, String token, PumpCompleteHandler handler) {
        StringBuilder b = Points.formatPoints(data);
        send(repo, b, token, handler);
    }

    private void send(String repo, StringBuilder builder, String token, final PumpCompleteHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("no CompletionHandler");
        }
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("no token");
        }
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException("no repo");
        }
        byte[] data = builder.toString().getBytes();
        StringMap headers = new StringMap();
        headers.put(HTTPHeaderAuthorization, token);
        headers.put(Client.ContentTypeHeader, TEXT_PLAIN);
        client.asyncPost(url(repo), data, headers, null, data.length, null, new CompletionHandler() {
            @Override
            public void complete(ResponseInfo info, JSONObject response) {
                handler.complete(info);
            }
        }, null);
    }

    private String url(String repo) {
        return config.pipelineHost + "/v2/repos/" + repo + "/data";
    }

    public interface PumpCompleteHandler {
        void complete(ResponseInfo info);
    }

}
