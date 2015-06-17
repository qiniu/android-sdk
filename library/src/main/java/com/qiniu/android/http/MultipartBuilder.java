/*
    This code is taken from Rafael Sanches' blog.
    http://blog.rafaelsanches.com/2011/01/29/upload-using-multipart-post-using-httpclient-in-android/
*/

package com.qiniu.android.http;

import com.qiniu.android.utils.StringUtils;

import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;


final class MultipartBuilder {

    private static final String STR_CR_LF = "\r\n";
    private static final byte[] CR_LF = {'\r', '\n'};
    private static final byte[] TRANSFER_ENCODING_BINARY =
            ("Content-Transfer-Encoding: binary" + STR_CR_LF).getBytes();

    private final static char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final String boundary;
    private final byte[] boundaryLine;
    private final byte[] boundaryEnd;


    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public MultipartBuilder() {
        final StringBuilder buf = new StringBuilder();
        final Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            buf.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }

        boundary = buf.toString();
        boundaryLine = ("--" + boundary + STR_CR_LF).getBytes();
        boundaryEnd = ("--" + boundary + "--" + STR_CR_LF).getBytes();

    }

    /**
     * Appends a quoted-string to a StringBuilder.
     * <p/>
     * <p>RFC 2388 is rather vague about how one should escape special characters
     * in form-data parameters, and as it turns out Firefox and Chrome actually
     * do rather different things, and both say in their comments that they're
     * not really sure what the right approach is. We go with Chrome's behavior
     * (which also experimentally seems to match what IE does), but if you
     * actually want to have a good chance of things working, please avoid
     * double-quotes, newlines, percent signs, and the like in your field names.
     */
    private static StringBuilder appendQuotedString(StringBuilder target, String key) {
        target.append('"');
        for (int i = 0, len = key.length(); i < len; i++) {
            char ch = key.charAt(i);
            switch (ch) {
                case '\n':
                    target.append("%0A");
                    break;
                case '\r':
                    target.append("%0D");
                    break;
                case '"':
                    target.append("%22");
                    break;
                default:
                    target.append(ch);
                    break;
            }
        }
        target.append('"');
        return target;
    }

    public void addPart(String key, String value, String contentType) {
        try {
            out.write(boundaryLine);
            out.write(createContentDisposition(key));
            out.write(createContentType(contentType));
            out.write(CR_LF);
            out.write(value.getBytes());
            out.write(CR_LF);
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    public void addPartWithCharset(String key, String value, String charset) {
        if (charset == null) charset = HTTP.UTF_8;
        addPart(key, value, "text/plain; charset=" + charset);
    }

    public void addPart(String key, String value) {
        addPartWithCharset(key, value, null);
    }

    public void addPart(String key, File file) throws IOException {
        addPart(key, file, null, file.getName());
    }


    public void addPart(String key, File file, String type, String customFileName) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        IOException e = null;
        try {
            addPart(key, customFileName, fis, type);
        } catch (IOException e1) {
            e = e1;
        }
        fis.close();
        if (e != null) {
            throw e;
        }
    }

    public void addPart(String key, String streamName, InputStream inputStream, String type)
            throws IOException {

        out.write(boundaryLine);

        // Headers
        out.write(createContentDisposition(key, streamName));
        out.write(createContentType(type));
        out.write(TRANSFER_ENCODING_BINARY);
        out.write(CR_LF);

        // Stream (file)
        final byte[] tmp = new byte[1024 * 8];
        int l;
        while ((l = inputStream.read(tmp)) != -1) {
            out.write(tmp, 0, l);
        }

        out.write(CR_LF);
    }

    private String normalizeContentType(String type) {
        return type == null ? HTTP.DEFAULT_CONTENT_TYPE : type;
    }

    private byte[] createContentType(String type) {
        String result = HTTP.CONTENT_TYPE + ": " + normalizeContentType(type) + STR_CR_LF;
        return result.getBytes();
    }

    private byte[] createContentDisposition(String key) {
        StringBuilder builder = new StringBuilder("Content-Disposition: form-data; name=");
        appendQuotedString(builder, key);
        builder.append(STR_CR_LF);
        return StringUtils.utf8Bytes(builder.toString());
    }

    private byte[] createContentDisposition(String key, String fileName) {
        StringBuilder builder = new StringBuilder("Content-Disposition: form-data; name=");
        appendQuotedString(builder, key);
        builder.append("; filename=");
        appendQuotedString(builder, fileName);
        builder.append(STR_CR_LF);
        return StringUtils.utf8Bytes(builder.toString());
    }

    public ByteArrayEntity build(ProgressHandler progressHandler, CancellationHandler c) {
        try {
            out.write(boundaryEnd);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        byte[] data = out.toByteArray();
        ByteArrayEntity b = new ByteArrayEntity(data, progressHandler, c);
        b.setContentType(new BasicHeader(
                HTTP.CONTENT_TYPE,
                "multipart/form-data; boundary=" + boundary));
        return b;
    }
}
