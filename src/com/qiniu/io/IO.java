package com.qiniu.io;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


import com.qiniu.auth.CallRet;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.conf.Conf;
import com.qiniu.utils.IOnProcess;
import com.qiniu.utils.InputStreamAt;
import com.qiniu.utils.MultipartEntity;
import com.qiniu.utils.FileUri;
import com.qiniu.utils.QiniuException;
import com.qiniu.utils.RetryRet;

public class IO {

	public static String UNDEFINED_KEY = null;
	private static Client mClient;
	private static String mUptoken;
	private static long mClientUseTime;
	public IO(Client client, String uptoken) {
		mClient = client;
		mUptoken = uptoken;
	}

	private static Client defaultClient() {
		if (mClient != null && System.currentTimeMillis() - mClientUseTime > 3 * 60 * 1000) { // 1 minute
			mClient.close();
			mClient = null;
		}
		if (mClient == null) {
			mClient = Client.defaultClient();
		}
		mClientUseTime = System.currentTimeMillis();
		return mClient;
	}

	private static MultipartEntity buildMultipartEntity(String key, InputStreamAt isa, PutExtra extra) throws IOException {
		MultipartEntity m = new MultipartEntity();
		if (key != null) {
			m.addField("key", key);
		}
		if (extra.checkCrc == PutExtra.AUTO_CRC32) {
			extra.crc32 = isa.crc32();
		}
		if (extra.checkCrc != PutExtra.UNUSE_CRC32) {
			m.addField("crc32", extra.crc32 + "");
		}
		for (Map.Entry<String, String> i: extra.params.entrySet()) {
			m.addField(i.getKey(), i.getValue());
		}

		m.addField("token", mUptoken);
		m.addFile("file", extra.mimeType, key == null ? "?" : key, isa);
		return m;
	}

	/**
	 * 上传二进制
	 *
	 * @param key	  键值名, UNDEFINED_KEY 表示自动生成key
	 * @param isa	  二进制数据
	 * @param extra   上传参数
	 * @param ret	  回调函数
	 */
	public void put(final String key, final InputStreamAt isa, final PutExtra extra, final JSONObjectRet ret) {
		final MultipartEntity m;
		try {
			m = buildMultipartEntity(key, isa, extra);
		} catch (IOException e) {
			ret.onFailure(new QiniuException(QiniuException.IO, "build multipart", e));
			return;
		}

		final Client client = defaultClient();
		final Client.ClientExecutor executor = client.makeClientExecutor();
		m.setProcessNotify(new IOnProcess() {
			@Override
			public void onProcess(long current, long total) {
				executor.upload(current, total);
			}

			@Override
			public void onFailure(QiniuException ex) {
				executor.onFailure(ex);
			}
		});

		CallRet retryRet = new RetryRet(ret){
			@Override
			public void onFailure(QiniuException ex) {
				if (RetryRet.noRetry(ex)){
					ret.onFailure(ex);
					return;
				}
				isa.reset();
				final Client.ClientExecutor executor2 = client.makeClientExecutor();
				client.call(executor2, Conf.UP_HOST2, m, ret);
			}
		};
		client.call(executor, Conf.UP_HOST, m, retryRet);
	}

	/**
	 * 通过提供URI来上传指定的文件
	 *
	 * @param mContext
	 * @param key
	 * @param uri 通过图库或其他拿到的URI
	 * @param extra 上传参数
	 * @param ret 结果回调函数
	 */
	public void putFile(Context mContext, String key, Uri uri, PutExtra extra, JSONObjectRet ret) {
		File file = FileUri.getFile(mContext, uri);
		if (!file.exists()) {
			ret.onFailure(QiniuException.fileNotFound(uri.toString()));
			return;
		}
		putFile(key, file, extra, ret);
	}

	public void putFile(String key, File file, PutExtra extra, JSONObjectRet ret) {
		putAndClose(key, InputStreamAt.fromFile(file), extra, ret);
	}

	private void putAndClose(final String key, final InputStreamAt input, final PutExtra extra, final JSONObjectRet ret) {
		JSONObjectRet closer = new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				input.close();
				ret.onSuccess(obj);
			}

			@Override
			public void onProcess(long current, long total) {
				ret.onProcess(current, total);
			}

			@Override
			public void onPause(Object tag) {
				ret.onPause(tag);
			}

			@Override
			public void onFailure(QiniuException ex) {
				input.close();
				ret.onFailure(ex);
			}
		};
		put(key, input, extra, closer);
	}

	public static void put(String uptoken, String key, InputStreamAt input, PutExtra extra, JSONObjectRet callback) {
		new IO(defaultClient(), uptoken).put(key, input, extra, callback);
	}

	public static void putFile(Context mContext, String uptoken, String key, Uri uri, PutExtra extra, JSONObjectRet callback) {
		new IO(defaultClient(), uptoken).putFile(mContext, key, uri, extra, callback);
	}
	public static void putFile(String uptoken, String key, File file, PutExtra extra, JSONObjectRet callback) {
		new IO(defaultClient(), uptoken).putFile(key, file, extra, callback);
	}
}

