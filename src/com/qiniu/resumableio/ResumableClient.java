package com.qiniu.resumableio;

import android.util.Base64;
import com.qiniu.auth.CallRet;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.conf.Conf;
import com.qiniu.utils.ICancel;
import com.qiniu.utils.InputStreamAt;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class ResumableClient extends Client {
    String mUpToken;
    int CHUNK_SIZE = 256 * 1024;
    int BLOCK_SIZE = 4 * 1024 * 1024;
	public ResumableClient(HttpClient client, String uptoken) {
		super(client);
        mUpToken = uptoken;
	}

	@Override
	protected HttpResponse roundtrip(HttpPost httpPost) throws IOException {
		if (mUpToken != null) {
			httpPost.setHeader("Authorization", "UpToken " + mUpToken);
		}
		return super.roundtrip(httpPost);
	}

    public ICancel[] putblock(final InputStreamAt input, final PutRet putRet, long offset, final JSONObjectRet callback) {
        final long writeNeed = Math.min(input.length()-offset, BLOCK_SIZE);
        final ICancel[] canceler = new ICancel[] {null};
        JSONObjectRet ret = new JSONObjectRet() {
            @Override
            public void onSuccess(JSONObject obj) {
                putRet.parse(obj);
                if (putRet.offset == writeNeed) {
                    callback.onSuccess(obj);
                    return;
                }
                canceler[0] = bput(putRet.host, input, putRet.ctx, putRet.offset, this);
            }

            @Override
            public void onProcess(long current, long total) {
                callback.onProcess(current, writeNeed);
            }

            @Override
            public void onFailure(Exception ex) {
                callback.onFailure(ex);
            }
        };
        if (putRet.isInvalid()) {
            canceler[0] = mkblk(input, offset, ret);
        } else {
            canceler[0] = bput(putRet.host, input, putRet.ctx, putRet.offset, ret);
        }
        return canceler;
    }

    public ICancel mkblk(InputStreamAt input, long offset, CallRet ret) {
        int remainLength = Math.min((int) (input.length() - offset), BLOCK_SIZE);
        String url = Conf.UP_HOST + "/mkblk/" + remainLength;
        remainLength = Math.min((int) (input.length()-offset), CHUNK_SIZE);
        return call(url, input.toHttpEntity(offset, remainLength, ret), ret);
    }

    public ICancel bput(String host, InputStreamAt input, String ctx, long offset, CallRet ret) {
        int remainLength = Math.min((int) (input.length() - offset), CHUNK_SIZE);
        String url = host + "/bput/" + ctx + "/" + offset;
        return call(url, input.toHttpEntity(offset, remainLength, ret), ret);
    }

    public ICancel mkfile(String key, long fsize, String mimeType, Map<String, String> params, String ctxs, CallRet ret) {
        String url = Conf.UP_HOST + "/mkfile/" + fsize;
        if (mimeType != null) {
            url += "/mimeType/" + encode(mimeType);
        }
        if (key != null) {
            url += "/key/" + encode(key);
        }
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> a: params.entrySet()) {
                url += "/" + a.getKey() + "/" + encode(a.getValue());
            }
        }
        StringEntity se;
        try {
            se = new StringEntity(ctxs);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            ret.onFailure(e);
            return null;
        }
        return call(url, se, ret);
    }

    public String encode(String data) {
        return Base64.encodeToString(data.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }
}
