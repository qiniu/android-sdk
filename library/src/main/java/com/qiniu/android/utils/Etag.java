package com.qiniu.android.utils;

import com.qiniu.android.storage.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 计算文件内容或者二进制数据的etag, etag算法是七牛用来标志数据唯一性的算法。
 * 文档：<a href="https://github.com/qiniu/qetag">etag算法</a>
 */
public final class Etag {

    /**
     * 计算二进制数据的etag
     *
     * @param data   二进制数据
     * @param offset 起始字节索引
     * @param length 需要计算的字节长度
     * @return 二进制数据的etag
     */
    public static String data(byte[] data, int offset, int length) {
        try {
            return stream(new ByteArrayInputStream(data, offset, length), length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // never reach
        return null;
    }

    /**
     * 计算二进制数据的etag
     *
     * @param data 二进制数据
     * @return 二进制数据的etag
     */
    public static String data(byte[] data) {
        return data(data, 0, data.length);
    }

    /**
     * 计算文件内容的etag
     *
     * @param file 文件对象
     * @return 文件内容的etag
     * @throws IOException 文件读取异常
     */
    public static String file(File file) throws IOException {
        FileInputStream fi = new FileInputStream(file);
        return stream(fi, file.length());
    }

    /**
     * 计算文件内容的etag
     *
     * @param filePath 文件路径
     * @return 文件内容的etag
     * @throws IOException 文件读取异常
     */
    public static String file(String filePath) throws IOException {
        File f = new File(filePath);
        return file(f);
    }

    /**
     * 计算输入流的etag
     *
     * @param in  数据输入流
     * @param len 数据流长度
     * @return 数据流的etag值
     * @throws IOException 文件读取异常
     */
    public static String stream(InputStream in, long len) throws IOException {
        if (len == 0) {
            return "Fto5o-5ea0sNMlW_75VgGJCv2AcJ";
        }
        byte[] buffer = new byte[64 * 1024];
        byte[][] blocks = new byte[(int) ((len + Configuration.BLOCK_SIZE - 1) / Configuration.BLOCK_SIZE)][];
        for (int i = 0; i < blocks.length; i++) {
            long left = len - (long) Configuration.BLOCK_SIZE * i;
            long read = left > Configuration.BLOCK_SIZE ? Configuration.BLOCK_SIZE : left;
            blocks[i] = oneBlock(buffer, in, (int) read);
        }
        return resultEncode(blocks);
    }

    /**
     * 单块计算hash
     *
     * @param buffer 数据缓冲区
     * @param in     输入数据
     * @param len    输入数据长度
     * @return 计算结果
     * @throws IOException 读取出错
     */
    private static byte[] oneBlock(byte[] buffer, InputStream in, int len) throws IOException {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("sha-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //never reach
            return null;
        }
        int buffSize = buffer.length;
        while (len != 0) {
            int next = buffSize > len ? len : buffSize;
            //noinspection ResultOfMethodCallIgnored
            in.read(buffer, 0, next);
            sha1.update(buffer, 0, next);
            len -= next;
        }

        return sha1.digest();
    }

    /**
     * 合并结果
     *
     * @param sha1s 每块计算结果的列表
     * @return 最终的结果
     */

    private static String resultEncode(byte[][] sha1s) {
        byte head = 0x16;
        byte[] finalHash = sha1s[0];
        int len = finalHash.length;
        byte[] ret = new byte[len + 1];
        if (sha1s.length != 1) {
            head = (byte) 0x96;
            MessageDigest sha1 = null;
            try {
                sha1 = MessageDigest.getInstance("sha-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                // never reach
                return null;
            }
            for (byte[] s : sha1s) {
                sha1.update(s);
            }
            finalHash = sha1.digest();
        }
        ret[0] = head;
        System.arraycopy(finalHash, 0, ret, 1, len);
        return UrlSafeBase64.encodeToString(ret);
    }
}
