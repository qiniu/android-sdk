package com.qiniu.qbox.up;

public class Config {

	public static String ACCESS_KEY	= "<Please apply your access key>";
	public static String SECRET_KEY	= "<Dont change here>";

	public static String REDIRECT_URI  = "<RedirectURL>";
	public static String AUTHORIZATION_ENDPOINT = "<AuthURL>";
	public static String TOKEN_ENDPOINT = "https://acc.qbox.me/oauth2/token";

	public static String IO_HOST = "http://iovip.qbox.me";
	public static String FS_HOST = "https://fs.qbox.me";
	public static String RS_HOST = "http://rs.qbox.me:10100";
	public static String UP_HOST = "http://up.qbox.me";

	public static int BLOCK_SIZE = 1024 * 1024 * 4;
	public static int PUT_CHUNK_SIZE = 1024 * 256;
	public static int PUT_RETRY_TIMES = 3;
	public static int PUT_TIMEOUT = 300000; // 300s = 5m
}