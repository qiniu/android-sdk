package com.qiniu.android.bigdata.pipeline;

import com.qiniu.android.bigdata.Configuration;
import com.qiniu.android.http.Client;
import com.qiniu.android.http.CompletionHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.utils.StringMap;

import org.json.JSONObject;

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

    public void pump(String repo, Map<String, Object> data, String token, PumpCompleteHandler handler) {
        Point p = Point.fromPointMap(data);
        sendPoint(repo, p, token, handler);
    }

    public void pump(String repo, Object data, String token, PumpCompleteHandler handler) {
        Point p = Point.fromPointObject(data);
        sendPoint(repo, p, token, handler);
    }

    public void pumpArray(String repo, Map<String, Object>[] data, String token, PumpCompleteHandler handler) {
        Batch b = Batch.fromMapArray(data);
        send(repo, b, token, handler);
    }

    public void pumpArray(String repo, Object[] data, String token, PumpCompleteHandler handler) {
        Batch b = Batch.fromObjectArray(data);
        send(repo, b, token, handler);
    }

    private void send(String repo, Batch b, String token, final PumpCompleteHandler handler) {
        byte[] data = b.toString().getBytes();
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

    private void sendPoint(String repo, Point p, String token, PumpCompleteHandler handler) {
        Batch b = new Batch();
        b.add(p);
        send(repo, b, token, handler);
    }


    public interface PumpCompleteHandler {
        void complete(ResponseInfo info);
    }

}
