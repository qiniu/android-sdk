package com.qiniu.io;

import android.content.Context;
import android.net.Uri;

import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.conf.Conf;
import com.qiniu.utils.MultipartFormData;
import com.qiniu.utils.Utils;

public class IO {
	
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
	 * @param uptoken
	 *            用于上传的验证信息
	 * @param key
	 *            键值名
	 * @param binary
	 *            二进制数据
	 * @param extra
	 *            上传参数
	 * @param ret
	 *            回调函数
	 */
	public static void put(String uptoken, String key,
			byte[] binary, PutExtra extra, JSONObjectRet ret) {
		String entryURI = extra.bucket + ":" + key;
		String url = Conf.UP_HOST + "/upload";

		MultipartFormData m = new MultipartFormData(binary.length + 1000);
		m.addField("auth", uptoken);

		StringBuffer action = new StringBuffer("/rs-put/"
				+ Utils.encodeUri(entryURI));
		if (Utils.isStringValid(extra.mimeType)) {
			action.append("/mimeType/" + Utils.encodeUri(extra.mimeType));
		}

		if (Utils.isStringValid(extra.customMeta)) {
			action.append("/meta/" + Utils.encodeUri(extra.customMeta));
		}
		m.addField("action", action.toString());

		m.addFile("file", key, binary);

		if (Utils.isStringValid(extra.callbackParams)) {
			m.addField("params", extra.callbackParams);
		}

		defaultClient().call(url, m.getContentType(), m.getEntity(), ret);
	}

	/**
	 * 通过提供URI来上传指定的文件
	 * 
	 * @param mContext
	 * @param uptoken
	 *            用于上传的验证信息
	 * @param key
	 * @param uri
	 *            通过图库或其他拿到的URI
	 * @param extra
	 *            上传参数
	 * @param ret
	 *            结果回调函数
	 */
	public static void putFile(Context mContext, String uptoken,
			String key, Uri uri, PutExtra extra, JSONObjectRet ret) {

		byte[] binaryData = Utils.readBinaryFromUri(mContext, uri);
		if (binaryData == null) {
			ret.onFailure(new Exception("URI有误, 无法读取指定数据"));
			return;
		}

		put(uptoken, key, binaryData, extra, ret);
	}

}
