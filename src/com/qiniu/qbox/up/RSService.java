package com.qiniu.qbox.up;

import java.io.File;
import java.util.Map;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.FileEntity;

import com.qiniu.qbox.auth.CallRet;
import com.qiniu.qbox.auth.Client;

public class RSService {
	private static final String DEFAULT_MIME_TYPE = "application/octet-stream" ;
	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream" ;
	private static final String DEFAULT_ROTATE = "0" ;
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
			mimeType = DEFAULT_MIME_TYPE ;
		}

		String url = Config.IO_HOST + "/rs-put/" + Client.urlsafeEncode(entryURI) + 
				"/mimeType/" + Client.urlsafeEncode(mimeType) ;
		if (customMeta != null && customMeta.length() != 0) {
			url += "/meta/" + Client.urlsafeEncode(customMeta);
		}

		if (rotate == null || rotate.length() == 0) {
			rotate = DEFAULT_ROTATE;
		}

		url += "/rotate/" + Client.urlsafeEncode(rotate);
		
		CallRet ret = conn.callWithBinary(url, entity) ;
		return new PutFileRet(ret) ;
	}
	
	public PutFileRet putFile(String key, String localFile, Map<String, Object> optParams) throws Exception {
		File f = new File(localFile) ;
		FileEntity entity = new FileEntity(f, APPLICATION_OCTET_STREAM) ;
		
		String mimeType = "" ;
		String customMeta = "" ;
		String rotate = "" ;
		
		if (optParams != null) {
			mimeType = (String)optParams.get("MIME_TYPE") ;
			customMeta = (String)optParams.get("CUSTOM_META") ;
			rotate = (String)optParams.get("rotate") ;
		}
		
		return  put(key, mimeType, entity, customMeta, rotate);
	}
}
