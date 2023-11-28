package com.qiniu.android.utils;

import com.qiniu.android.common.Constants;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * 字符串连接工具类
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 以指定的分隔符来进行字符串元素连接
     * 例如有字符串数组array和连接符为逗号(,)
     * <code>
     * String[] array = new String[] { "hello", "world", "qiniu", "cloud","storage" };
     * </code>
     * 那么得到的结果是:
     * <code>
     * hello,world,qiniu,cloud,storage
     * </code>
     *
     * @param array 需要连接的字符串数组
     * @param sep   元素连接之间的分隔符
     * @return 连接好的新字符串
     */
    public static String join(String[] array, String sep) {
        if (array == null) {
            return null;
        }

        int arraySize = array.length;
        int sepSize = 0;
        if (sep != null && !sep.equals("")) {
            sepSize = sep.length();
        }

        int bufSize = (arraySize == 0 ? 0 : ((array[0] == null ? 16 : array[0].length()) + sepSize) * arraySize);
        StringBuilder buf = new StringBuilder(bufSize);

        for (int i = 0; i < arraySize; i++) {
            if (i > 0) {
                buf.append(sep);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    /**
     * 以json元素的方式连接字符串中元素
     * 例如有字符串数组array
     * <code>
     * String[] array = new String[] { "hello", "world", "qiniu", "cloud","storage" };
     * </code>
     * 那么得到的结果是:
     * <code>
     * "hello","world","qiniu","cloud","storage"
     * </code>
     *
     * @param array 需要连接的字符串数组
     * @return 以json元素方式连接好的新字符串
     */
    public static String jsonJoin(String[] array) {
        int arraySize = array.length;
        if (array[0] == null) {
            array[0] = "";
        }
        int bufSize = arraySize * (array[0].length() + 3);
        StringBuilder buf = new StringBuilder(bufSize);
        for (int i = 0; i < arraySize; i++) {
            if (i > 0) {
                buf.append(',');
            }

            buf.append('"');
            buf.append(array[i]);
            buf.append('"');
        }
        return buf.toString();
    }

    /**
     * json join
     *
     * @param array Long 数组
     * @return json string
     */
    public static String jsonJoin(Long[] array) {
        return jsonJoin(longToString(array));
    }

    /**
     * Long 数组转 String 数组
     *
     * @param longArray Long 数组
     * @return String 数组
     */
    public static String[] longToString(Long longArray[]) {
        String stringArray[] = new String[longArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            try {
                stringArray[i] = String.valueOf(longArray[i]);
            } catch (NumberFormatException e) {
                stringArray[i] = "null";
                continue;
            }
        }
        return stringArray;

    }

    /**
     * 获取 utf8 数据
     *
     * @param data 原数据
     * @return utf8 数据
     */
    public static byte[] utf8Bytes(String data) {
        try {
            return data.getBytes(Constants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * 判断字符串是否为空
     *
     * @param s 字符串
     * @return 是否为空
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * 转换成非空字符串
     *
     * @param s 待转对象
     * @return String
     */
    public static String toNonnullString(Object s) {
        return s == null ? "" : "" + s;
    }

    /**
     * 判断字符串是否为空或者仅包含空格
     *
     * @param s 字符串
     * @return 判断结果
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }

    /**
     * strip
     *
     * @param s 待处理字符串
     * @return strip 后的字符串
     */
    public static String strip(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0, length = s.length(); i < length; i++) {
            char c = s.charAt(i);
            if (c > '\u001f' && c < '\u007f') {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * 对象转数组
     *
     * @param obj obj
     * @return byte array
     */
    public static byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                try {
                    oos.writeObject(obj);
                    oos.flush();
                    bytes = bos.toByteArray();
                } finally {
                    oos.close();
                }
            } finally {
                bos.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bytes;
    }

    /**
     * 数组转对象
     *
     * @param bytes bytes
     * @return Object
     */
    public static Object toObject(byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            try {
                ObjectInputStream ois = new ObjectInputStream(bis);
                obj = ois.readObject();
                ois.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                bis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * 获取 Token 中的 AK + Scope
     *
     * @param token 上传的 Token
     * @return Token 中的 AK + Scope
     */
    public static String getAkAndScope(String token) {
        String[] strings = token.split(":");
        String ak = strings[0];
        String policy = null;
        try {
            policy = new String(UrlSafeBase64.decode(strings[2]), Constants.UTF_8);
            JSONObject obj = new JSONObject(policy);
            String scope = obj.getString("scope");
            String bkt = scope.split(":")[0];
            return ak + bkt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 Token 中的 Bucket
     *
     * @param token 上传 Token
     * @return Bucket
     */
    public static String getBucket(String token) {
        String[] strings = token.split(":");
        String policy = null;
        try {
            policy = new String(UrlSafeBase64.decode(strings[2]), Constants.UTF_8);
            JSONObject obj = new JSONObject(policy);
            String scope = obj.getString("scope");
            String bkt = scope.split(":")[0];
            return bkt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 转大写
     *
     * @param str str
     * @return 转之后的字符串
     */
    public static String upperCase(String str) {
        if (str.length() <= 0 || str == null) {
            return "";
        }
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }

}
