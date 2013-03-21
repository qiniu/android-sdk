package com.qiniu.up;

import android.content.Context;
import android.net.Uri;
import com.qiniu.auth.JSONObjectRet;
import com.qiniu.auth.UpAuth;
import com.qiniu.conf.Conf;
import com.qiniu.utils.MultipartFormData;
import com.qiniu.utils.Utils;

public class Up {
	private String mUpToken;
	private UpAuth mUpAuth;
	private String mHost;

	public Up(String upToken, String host) {
		mUpAuth = new UpAuth(upToken);
		mHost = host;
		mUpToken = upToken;
	}

	public Up(String upToken) {
		this(upToken, Conf.UP_HOST);
	}

	/**
	 * 上传二进制
	 *
	 * @param fileName 如果是null的话会随机生成一串6位的字符串
	 * @param opts 上传参数
	 * @param binary 二进制数据
	 * @param ret 回调函数
	 */
	public void Put(String fileName, UpOption opts, byte[] binary, String params, JSONObjectRet ret) {
		if ( ! Utils.IsStringValid(fileName)) {
			fileName = Utils.GetRandomString(6);
		}
		String url = mHost + "/upload";

		MultipartFormData m = new MultipartFormData(binary.length + 1000);
		m.addField("auth", mUpToken);

		String action = "/rs-put/" + opts.toUri();
		m.addField("action", action);

		String mimeType = opts.MimeType;
		if ( ! Utils.IsStringValid(mimeType)) {
			mimeType = "application/octet-stream";
		}
		m.addFile("file", fileName, mimeType, binary);

		if (Utils.IsStringValid(params)) {
			m.addField("params", params);
		}

		mUpAuth.Call(url, m.getContentType(), m.getEntity(), ret);
	}

	/**
	 * 通过提供URI来上传指定的文件
	 *
	 * @param mContext
	 * @param uri 通过图库或其他拿到的URI
	 * @param fileName 如果是null的话会随机生成一串6位的字符串
	 * @param opts 上传参数
	 * @param ret 结果回调函数
	 */
	public void PutFile(Context mContext, Uri uri, String fileName, UpOption opts, String params, JSONObjectRet ret) {
		byte[] binaryData = Utils.ReadBinaryFromUri(mContext, uri);
		if (binaryData == null) {
			ret.onFailure(new Exception("URI有误, 无法读取制定数据"));
			return;
		}

		Put(fileName, opts, binaryData, params, ret);
	}

}
