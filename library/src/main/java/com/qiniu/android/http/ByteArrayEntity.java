package com.qiniu.android.http;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ByteArrayEntity extends AbstractHttpEntity implements Cloneable {

    private final byte[] b;
    private final int off, len;


    public ByteArrayEntity(final byte[] b) {
        super();
        this.b = b;
        this.off = 0;
        this.len = this.b.length;
    }

    public ByteArrayEntity(final byte[] b, final int off, final int len) {
        super();
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) < 0) || ((off + len) > b.length)) {
            throw new IndexOutOfBoundsException("off: " + off + " len: " + len + " b.length: " + b.length);
        }
        this.b = b;
        this.off = off;
        this.len = len;
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
    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(this.b, this.off, this.len);
        outstream.flush();
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