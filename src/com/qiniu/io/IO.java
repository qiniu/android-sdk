package com.qiniu.io;

import android.content.Context;
import android.net.Uri;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.conf.Conf;
import com.qiniu.utils.InputStreamAt;
import com.qiniu.utils.MultipartEntity;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Map;

public class IO {

	public static String UNDEFINED_KEY = "?";

	private static Client mClient;

	public static void setClient(Client client) {
		mClient = client;
	}

	private static Client defaultClient() {
		if (mClient == null) {
			mClient = Client.defaultClient();
		}
		return mClient;
	}

	/**
	 * 上传二进制
	 *
	 * @param uptoken 用于上传的验证信息
	 * @param key     键值名, UNDEFINED_KEY 表示自动生成key
	 * @param isa     二进制数据
	 * @param extra   上传参数
	 * @param ret     回调函数
	 */
	public static void put(String uptoken, String key, InputStreamAt isa, PutExtra extra, JSONObjectRet ret) {

		MultipartEntity m = new MultipartEntity();
		if (key == null) {
			key = UNDEFINED_KEY;
		}
		if ( ! key.equals(UNDEFINED_KEY)) {
			m.addField("key", key);
		}
		
		if (uptoken == null || uptoken.length() == 0) {
			ret.onFailure(new Exception("uptoken未提供"));
			return;
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

		m.addField("token", uptoken);
		m.addFile("file", extra.mimeType, key, isa);


		defaultClient().call(Conf.UP_HOST, m, ret);
	}

	/**
	 * 通过提供URI来上传指定的文件
	 *
	 * @param mContext
	 * @param uptoken 用于上传的验证信息
	 * @param key
	 * @param uri 通过图库或其他拿到的URI
	 * @param extra 上传参数
	 * @param ret 结果回调函数
	 */
	public static void putFile(Context mContext, String uptoken, String key, Uri uri, PutExtra extra, final JSONObjectRet ret) {

		final InputStreamAt isa;
		try {
			isa = InputStreamAt.fromInputStream(mContext, mContext.getContentResolver().openInputStream(uri));
		} catch (FileNotFoundException e) {
			ret.onFailure(e);
			return;
		}
		
		put(uptoken, key, isa, extra, new JSONObjectRet() {
			@Override
			public void onSuccess(JSONObject obj) {
				isa.close();
				ret.onSuccess(obj);
			}
			
			@Override
			public void onFailure(Exception ex) {
				isa.close();
				ret.onFailure(ex);
			}
		});
	}
}
