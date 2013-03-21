package com.qiniu.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class Utils {
	public static String GetRandomString(int length) {
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	public static boolean IsStringValid(String str) {
		return str != null && str.length() > 0;
	}

	public static String EncodeUri(String uri) {
		return new String(Base64.encode(uri.getBytes(), Base64.URL_SAFE)).trim();
	}

	public static byte[] ReadBinaryFromUri(Context mContext, Uri uri) {
		ContentResolver cr = mContext.getContentResolver();
		try {
			InputStream imgIS = imgIS = cr.openInputStream(uri);
			byte[] dataBinary = new byte[imgIS.available()];
			int readed = imgIS.read(dataBinary);
			while (readed < dataBinary.length) {
				readed += imgIS.read(dataBinary, readed, dataBinary.length - readed);
			}
			return dataBinary;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
