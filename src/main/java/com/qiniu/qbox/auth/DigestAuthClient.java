package com.qiniu.qbox.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;

import com.qiniu.qbox.Config;

public class DigestAuthClient extends Client {

	@Override
	public void setAuth(HttpPost post) {
		URI uri = post.getURI();
		String path = uri.getRawPath();
		String query = uri.getRawQuery();
		HttpEntity entity = post.getEntity();

		byte[] secretKey = Config.SECRET_KEY.getBytes();
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA1");
			mac.init(keySpec);
			mac.update(path.getBytes());
			if (query != null && query.length() != 0) {
				mac.update((byte)('?'));
				mac.update(query.getBytes());
			}
			mac.update((byte)'\n');
			if (entity != null) {
				org.apache.http.Header ct = entity.getContentType();
				if (ct != null && ct.getValue() == "application/x-www-form-urlencoded") {
					ByteArrayOutputStream w = new ByteArrayOutputStream();
					entity.writeTo(w);
					mac.update(w.toByteArray());
				}
			}

			byte[] digest = mac.doFinal();
			byte[] digestBase64 = Client.urlsafeEncodeBytes(digest);

			StringBuffer b = new StringBuffer();
			b.append("QBox ");
			b.append(Config.ACCESS_KEY);
			b.append(':');
			b.append(new String(digestBase64));

			post.setHeader("Authorization", b.toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
