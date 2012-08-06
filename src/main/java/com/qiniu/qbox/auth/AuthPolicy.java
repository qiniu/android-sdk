package com.qiniu.qbox.auth;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONStringer;

import com.qiniu.qbox.Config;

public class AuthPolicy {
	public String scope;
	public String callbackUrl;
	public String returnUrl;
	public long deadline;
	
	public AuthPolicy(String scope, long expires) {
		this.scope = scope;
		this.deadline = System.currentTimeMillis() / 1000 + expires;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}

	public String marshal() throws JSONException {

		JSONStringer stringer = new JSONStringer();
		
		stringer.object();
		stringer.key("scope").value(this.scope);
		if (this.callbackUrl != null) {
			stringer.key("callbackUrl").value(this.callbackUrl);
		}
		if (this.returnUrl != null) {
			stringer.key("returnUrl").value(this.returnUrl);
		}
		stringer.key("deadline").value(this.deadline);
		stringer.endObject();

		return stringer.toString();
	}

	public byte[] makeAuthToken() {

		byte[] accessKey = Config.ACCESS_KEY.getBytes();
		byte[] secretKey = Config.SECRET_KEY.getBytes();
		try {
			String policyJson = this.marshal();
			byte[] policyBase64 = Client.urlsafeEncodeBytes(policyJson.getBytes());

			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA1");
			mac.init(keySpec);

			byte[] digest = mac.doFinal(policyBase64);
			byte[] digestBase64 = Client.urlsafeEncodeBytes(digest);			
			byte[] token = new byte[accessKey.length + 30 + policyBase64.length];

			System.arraycopy(accessKey, 0, token, 0, accessKey.length);
			token[accessKey.length] = ':';
			System.arraycopy(digestBase64, 0, token, accessKey.length + 1, digestBase64.length);
			token[accessKey.length + 29] = ':';
			System.arraycopy(policyBase64, 0, token, accessKey.length + 30, policyBase64.length);

			return token;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String makeAuthTokenString() {
		byte[] authToken = this.makeAuthToken();
		return new String(authToken);
	}
}
