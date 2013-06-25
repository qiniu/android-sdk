package com.qiniu.io;

import java.util.HashMap;

public class PutExtra {
	public static int UNUSE_CRC32 = 0;
	public static int AUTO_CRC32 = 1;
	public static int SPECIFY_CRC32 = 2;
	
	public HashMap<String, String> params = new HashMap<String, String>(); // key要以x:开头
	public String mimeType;
	public long crc32;
	public int checkCrc = UNUSE_CRC32;
}
