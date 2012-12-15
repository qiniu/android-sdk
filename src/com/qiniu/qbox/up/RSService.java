package com.qiniu.qbox.up;

import java.io.File;
import java.util.Map;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.FileEntity;

import com.qiniu.qbox.auth.CallRet;
import com.qiniu.qbox.auth.Client;

public class RSService {
	private Client conn ;
	private String bucketName ;
	
	public RSService(Client conn, String bucketName) {
		this.conn = conn ;
		this.bucketName = bucketName ;
	}
	
	public GetRet get(String key, String attName) throws Exception {
		String entryURI = this.bucketName + ":" + key;
		String url = Config.RS_HOST + "/get/" + Client.urlsafeEncode(entryURI) + "/attName/"
				+ Client.urlsafeEncode(attName);
		CallRet callRet = conn.call(url);
		return new GetRet(callRet);
	}

	public PutAuthRet putAuth() throws Exception {
		CallRet ret = conn.call(Config.IO_HOST + "/put-auth/") ;
		return new PutAuthRet(ret) ;
	}
	
	private PutFileRet put(String key, String mimeType, AbstractHttpEntity entity, String customMeta, String rotate) throws Exception {
		String entryURI = this.bucketName + ":" + key ;
		if (mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream" ;
		}

		String url = Config.IO_HOST + "/rs-put/" + Client.urlsafeEncode(entryURI) + 
				"/mimeType/" + Client.urlsafeEncode(mimeType) ;
		if (customMeta != null && customMeta.length() != 0) {
			url += "/meta/" + Client.urlsafeEncode(customMeta);
		}

		if (rotate != null && rotate.length() != 0) {
			url += "/rotate/" + Client.urlsafeEncode(rotate);
		}

		CallRet ret = conn.callWithBinary(url, entity) ;
		return new PutFileRet(ret) ;
	}
	
	public PutFileRet putFile(String key, String localFile, Map<String, Object> optParams) throws Exception {
		File f = new File(localFile) ;
		FileEntity entity = new FileEntity(f, "application/octet-stream") ;
		
		String mimeType = "" ;
		String customMeta = "" ;
		String rotate = "" ;
		
		if (optParams != null) {
			mimeType = (String)optParams.get("MIME_TYPE") ;
			customMeta = (String)optParams.get("CUSTOM_META") ;
			rotate = (String)optParams.get("ROTATE") ;
		}
		
		return  put(key, mimeType, entity, customMeta, rotate);
	}
}
