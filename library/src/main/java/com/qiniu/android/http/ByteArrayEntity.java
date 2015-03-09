package com.qiniu.android.http;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 定义请求字节实体及相关方法
 */
public final class ByteArrayEntity extends AbstractHttpEntity implements Cloneable {

    private final byte[] b;
    private final int off, len;
    private final ProgressHandler progressHandler;
    private static final int progressStep = 8 * 1024;

    public ByteArrayEntity(final byte[] b, ProgressHandler h) {
        this(b, 0, b.length, h);
    }

    public ByteArrayEntity(final byte[] b, final int off, final int len, ProgressHandler h) {
        super();
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) < 0) || ((off + len) > b.length)) {
            throw new IndexOutOfBoundsException("off: " + off + " len: " + len + " b.length: " + b.length);
        }
        this.b = b;
        this.off = off;
        this.len = len;
        this.progressHandler = h;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return this.len;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(this.b, this.off, this.len);
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        if (progressHandler != null) {
            writeWithProgress(outStream);
        } else {
            outStream.write(this.b, this.off, len);
        }
        outStream.flush();
    }

    private void writeWithProgress(final OutputStream outStream) throws IOException {
        int off = 0;
        while (off < this.len) {
            int left = this.len - off;
            int len = left < progressStep ? left : progressStep;
            outStream.write(this.b, this.off + off, len);
            progressHandler.onProgress(off, this.len);
            off += len;
        }
    }

    /**
     * Tells that this entity is not streaming.
     *
     * @return {@code false}
     */
    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}