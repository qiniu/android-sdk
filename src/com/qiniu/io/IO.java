package com.qiniu.io;

import android.content.Context;
import android.net.Uri;
import com.qiniu.auth.Client;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.auth.UpAuth;
import com.qiniu.conf.Conf;
import com.qiniu.utils.MultipartFormData;
import com.qiniu.utils.Utils;

public class IO {
	private static Client mClient;
	public static void SetClient(Client client) {
		mClient = client;
	}

	private static Client DefaultClient() {
		if (mClient == null) {
			mClient = Client.DefaultClient();
		}
		return mClient;
	}

	/**
	 * 上传二进制
	 *
	 * @param uptoken 用于上传的验证信息
	 * @param bucket 仓库名称
	 * @param key 键值名
	 * @param binary 二进制数据
	 * @param extra 上传参数
	 * @param ret 回调函数
	 */
	public static void Put(String uptoken, String bucket, String key, byte[] binary, PutExtra extra, JSONObjectRet ret) {
		String entryURI = bucket + ":" + key;
		String url = Conf.UP_HOST + "/upload";

		MultipartFormData m = new MultipartFormData(binary.length + 1000);
		m.addField("auth", uptoken);

		StringBuffer action = new StringBuffer("/rs-put/" + Utils.EncodeUri(entryURI));
		if (Utils.IsStringValid(extra.MimeType)) {
			action.append("/mimeType/" + Utils.EncodeUri(extra.MimeType));
		}

		if (Utils.IsStringValid(extra.CustomMeta)) {
			action.append("/meta/" + Utils.EncodeUri(extra.CustomMeta));
		}
		m.addField("action", action.toString());

		m.addFile("file", key, binary);

		if (Utils.IsStringValid(extra.CallbackParams)) {
			m.addField("params", extra.CallbackParams);
		}

		DefaultClient().call(url, m.getContentType(), m.getEntity(), ret);
	}

	/**
	 * 通过提供URI来上传指定的文件
	 *
	 * @param mContext
	 * @param uptoken 用于上传的验证信息
	 * @param bucket
	 * @param key
	 * @param uri 通过图库或其他拿到的URI
	 * @param extra 上传参数
	 * @param ret 结果回调函数
	 */
	public static void PutFile(Context mContext, String uptoken, String bucket, String key, Uri uri,
							   PutExtra extra, JSONObjectRet ret) {

		byte[] binaryData = Utils.ReadBinaryFromUri(mContext, uri);
		if (binaryData == null) {
			ret.onFailure(new Exception("URI有误, 无法读取指定数据"));
			return;
		}

		Put(uptoken, bucket, key, binaryData, extra, ret);
	}

}
