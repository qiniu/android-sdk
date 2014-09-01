package com.qiniu.utils;

import java.util.zip.CRC32;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Crc32 {
	
    public static long calc(byte[] data, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, offset, length);
        return crc32.getValue();
    }

    public static long calc(byte[] data) {
        return calc(data, 0, data.length);
    }
    
    public static long calc(File file) throws IOException{
    	 FileInputStream fis = new FileInputStream(file);
    	 return calc(fis);
    }

    public static long calc(InputStream is) throws IOException{
        int blockSize = 64 * 1024;
        byte[] buffer = new byte[blockSize];
        int read = 0;
        CRC32 crc32 = new CRC32();
        while((read = is.read(buffer, 0, buffer.length)) > 0){
            crc32.update(buffer, 0, read);
        }
        return crc32.getValue();
    }

}
