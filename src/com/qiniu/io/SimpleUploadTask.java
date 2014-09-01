package com.qiniu.io;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;

import com.qiniu.auth.Authorizer;
import com.qiniu.conf.Conf;
import com.qiniu.rs.CallRet;
import com.qiniu.rs.CallBack;
import com.qiniu.rs.PutExtra;
import com.qiniu.utils.IOnProcess;
import com.qiniu.utils.InputStreamAt;
import com.qiniu.utils.MultipartEntity;
import com.qiniu.utils.UploadTask;
import com.qiniu.utils.Util;


public class SimpleUploadTask extends UploadTask {

	public SimpleUploadTask(Authorizer auth, InputStreamAt isa, String key,
			PutExtra extra, CallBack ret) throws IOException {
		super(auth, isa, key, extra, ret);
	}

	@Override
	protected CallRet execDoInBackground(Object... arg0) {
		CallRet ret = upload(Conf.UP_HOST);
		if(Util.needChangeUpAdress(ret)){
			try {
				orginIsa.reset();
				ret = upload(Conf.UP_HOST2);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	private CallRet upload(String url){
		try{
			post = Util.newPost(url);
			post.setEntity(buildHttpEntity());
			HttpResponse response = getHttpClient().execute(post);
			CallRet ret = Util.handleResult(response);
			return ret;
		}catch(Exception e){
			return new CallRet(Conf.ERROR_CODE, "", e);
		}finally{
			post = null;
		}
	}

	private MultipartEntity buildHttpEntity() throws IOException {
		MultipartEntity m = new MultipartEntity();
		if (key != null) {
			m.addField("key", key);
		}
		if (extra.checkCrc == PutExtra.AUTO_CRC32) {
			extra.crc32 = orginIsa.crc32();
		}
		if (extra.checkCrc != PutExtra.UNUSE_CRC32) {
			m.addField("crc32", extra.crc32 + "");
		}
		if(extra.params != null){
			for (Map.Entry<String, String> i: extra.params.entrySet()) {
				if(i.getKey().startsWith("x:")){
					m.addField(i.getKey(), i.getValue());
				}
			}
		}

		m.addField("token", auth.getUploadToken()); 
		m.addFile("file", extra.mimeType, orginIsa.getFilename(), orginIsa);
		
		
		m.setProcessNotify(new IOnProcess(){

			@Override
			public void onProcess(long current, long total) {
				publishProgress(current, total); 
			}

		});
		
		return m;
	}

}
