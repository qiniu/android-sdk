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

	public ICancel[] putblock(final InputStreamAt input, final PutRet putRet, final long offset, final JSONObjectRet callback) {
		final int writeNeed = (int) Math.min(input.length()-offset, BLOCK_SIZE);
		final ICancel[] canceler = new ICancel[] {null};
		final long[] wrote = new long[] {0, 0};
		final long[] crc32 = new long[] {0};
		JSONObjectRet ret = new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				if (writeNeed < BLOCK_SIZE) {
					int c = 1;
					c = 2;
				}
				PutRet pr = new PutRet();
				pr.parse(obj);
				if (crc32[0] != pr.crc32) {
					putblock(input, putRet, offset, callback);
//					  callback.onFailure(new Exception("not match"));
					return;
				}
				putRet.parse(obj);
				wrote[1] += wrote[0];
				wrote[0] = 0;
				if (putRet.offset == writeNeed) {
					callback.onSuccess(obj);
					return;
				}
				int remainLength = Math.min((int) (input.length() - offset - putRet.offset), CHUNK_SIZE);
				crc32[0] = input.getCrc32(offset+putRet.offset, remainLength);
				canceler[0] = bput(putRet.host, input, putRet.ctx, offset, putRet.offset, remainLength, this);
			}

			@Override
			public void onProcess(long current, long total) {
				wrote[0] = current;
				callback.onProcess(wrote[0]+wrote[1], writeNeed);
			}

			@Override
			public void onFailure(Exception ex) {
				callback.onFailure(ex);
			}
		};
		if (putRet.isInvalid()) {
			int chunkSize = Math.min(writeNeed, CHUNK_SIZE);
			crc32[0] = input.getCrc32(offset, chunkSize);
			canceler[0] = mkblk(input, offset, writeNeed, chunkSize, ret);
		} else {
			int remainLength = Math.min((int) (input.length() - offset - putRet.offset), CHUNK_SIZE);
			crc32[0] = input.getCrc32(offset+putRet.offset, remainLength);
			canceler[0] = bput(putRet.host, input, putRet.ctx, offset, putRet.offset, remainLength, ret);
		}
		return canceler;
	}

	public ICancel mkblk(InputStreamAt input, long offset, int blockSize, int chunkSize, CallRet ret) {
		int remainLength = blockSize;
		String url = Conf.UP_HOST + "/mkblk/" + remainLength;
		ClientExecuter client = makeClientExecuter();
		call(client, url, input.toHttpEntity(offset, chunkSize, client), ret);
		return client;
	}

	public ICancel bput(String host, InputStreamAt input, String ctx, long blockOffset, long offset, int writeLength, CallRet ret) {
		String url = host + "/bput/" + ctx + "/" + offset;
		ClientExecuter client = makeClientExecuter();
		call(client, url, input.toHttpEntity(blockOffset+offset, writeLength, client), ret);
		return client;
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
		ClientExecuter client = makeClientExecuter();
		call(client, url, se, ret);
		return client;
	}

	public String encode(String data) {
		return Base64.encodeToString(data.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
	}
}
