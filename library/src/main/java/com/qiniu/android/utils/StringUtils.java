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

    /**
     * 以指定的分隔符来进行字符串元素连接
     * <p>
     * 例如有字符串数组array和连接符为逗号(,)
     * <code>
     * String[] array = new String[] { "hello", "world", "qiniu", "cloud","storage" };
     * </code>
     * 那么得到的结果是:
     * <code>
     * hello,world,qiniu,cloud,storage
     * </code>
     * </p>
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
     * <p>
     * 例如有字符串数组array
     * <code>
     * String[] array = new String[] { "hello", "world", "qiniu", "cloud","storage" };
     * </code>
     * 那么得到的结果是:
     * <code>
     * "hello","world","qiniu","cloud","storage"
     * </code>
     * </p>
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


    public static String jsonJoin(Long[] array) {
        return jsonJoin(longToString(array));
    }

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

    public static byte[] utf8Bytes(String data) {
        try {
            return data.getBytes(Constants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || "".equals(s);
    }

    public static String toNonnullString(Object s) {
        return s == null ? "" : "" + s;
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }

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
     * @param obj
     * @return
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
     * @param bytes
     * @return
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
