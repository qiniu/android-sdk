package com.qiniu.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;

import com.qiniu.conf.Conf;
import com.qiniu.rs.CallRet;
public class Util {

	public static byte[] toByte(String s){
		try {
			return s.getBytes(Conf.CHARSET);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
    public static String urlsafeBase64(String data) {
        return Base64.encodeToString(toByte(data), Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    public static String urlsafeBase64(byte[] binaryData) {
        return Base64.encodeToString(binaryData, Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    public static CallRet handleResult(HttpResponse res) {
    	String reqId = null;
        try {
            StatusLine status = res.getStatusLine();
            Header header = res.getFirstHeader("X-Reqid");
            if(header != null){
            	reqId = header.getValue();
            }
            int statusCode = status.getStatusCode();
            String responseBody = EntityUtils.toString(
            		res.getEntity(), Conf.CHARSET);
            return new CallRet(statusCode, reqId, responseBody);
        } catch (Exception e) {
            return new CallRet(Conf.ERROR_CODE, reqId, e);
        }
    }
    
    public static HttpPost newPost(String url){
		HttpPost postMethod = new HttpPost(url);
		postMethod.setHeader("User-Agent", getUserAgent());
		return postMethod;
	}
    
    public static boolean needChangeUpAdress(CallRet ret){
		if(ret.getException() != null && ret.getException().reason != null){
			try {
				throw ret.getException().reason;
			} catch (java.net.UnknownHostException e) {

			} catch (java.net.NoRouteToHostException e) {
				
			} catch (java.net.PortUnreachableException e) {
				
			} catch (org.apache.http.conn.HttpHostConnectException e) {
				
			} catch (java.net.ConnectException e) {
				
			} catch (java.net.UnknownServiceException e) {
				
			} catch (org.apache.http.conn.ConnectTimeoutException e) {
				
			} catch (java.net.SocketTimeoutException e) {
				
			} catch (java.io.InterruptedIOException e) { //?
				
			} catch (Exception e) {
				return false;
			}
			
			return true;
		}
		return false;
	}
    
    public static File getSDPath(Context context){
        File sdDir = context.getCacheDir();
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        return sdDir;
    }
    
    public static File storeToFile(Context context, InputStream is) throws IOException {
		if (is == null) {
			return null;
		}
		OutputStream os = null;
		File f = null;
		try {
			File outputDir = getSDPath(context);
			f = File.createTempFile("qiniu-", "", outputDir);
			os = new FileOutputStream(f);
			byte[] buffer = new byte[64 * 1024];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			if (f != null) {
				f.delete();
				f = null;
			}
			throw e;
		}finally{
			try {is.close();} catch (Exception e){}
			if (os != null) {
				try {os.close();} catch (IOException e){}
			}
		}

		return f;
	}
    
    private static String userAgent;
    public static String getUserAgent(){
    	if(Conf.USER_AGENT != null){
    		return Conf.USER_AGENT;
    	}
    	if(userAgent == null){
			String sdk = "QiniuAndroid/" + Conf.VERSION;
			
			StringBuilder buffer = new StringBuilder("Linux; U; Android ");
	        final String version = Build.VERSION.RELEASE;
			buffer.append(version);
	        buffer.append(";");
	        
	        if ("REL".equals(Build.VERSION.CODENAME)) {
	            final String model = Build.MODEL;
	            final String brand = Build.BRAND;
	            if (brand.length() > 0) {
	                buffer.append(" ");
	                buffer.append(brand);
	            }
	            
	            if (model.length() > 0) {
	                buffer.append(" ");
	                buffer.append(model);
	            }
	        }
	        final String id = Build.ID;
	        if (id.length() > 0) {
	            buffer.append(" Build/");
	            buffer.append(id);
	        }

			userAgent = sdk + " (" + buffer + ")";
    	}
    	
		return userAgent;
	}
    
	
}
