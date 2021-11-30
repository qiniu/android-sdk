package com.qiniu.android.storage;

import com.qiniu.android.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;

class UploadSourceStream extends UploadSource {

    private long readOffset = 0;

    private InputStream inputStream;
    private String id;
    private boolean hasSize = false;
    private long size = UploadSource.UnknownSourceSize;
    private String fileName;

    UploadSourceStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    protected void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }


    void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return !StringUtils.isNullOrEmpty(id) ? id : fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getSize() {
        if (size > UnknownSourceSize) {
            return size;
        } else {
            return UnknownSourceSize;
        }
    }

    void setSize(long size) {
        this.hasSize = size > 0;
        this.size = size;
    }

    @Override
    public boolean couldReloadSource() {
        return false;
    }

    @Override
    public boolean reloadSource() {
        readOffset = 0;
        return false;
    }

    @Override
    public byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (inputStream == null) {
            throw new IOException("inputStream is empty");
        }

        byte[] buffer = null;
        synchronized (this) {
            boolean isEOF = false;
            while (true) {
                if (readOffset == dataOffset) {
                    int readSize = 0;
                    buffer = new byte[dataSize];
                    while (readSize < dataSize) {
                        int ret = inputStream.read(buffer, readSize, dataSize - readSize);
                        if (ret < 0) {
                            isEOF = true;
                            break;
                        }
                        readSize += ret;
                    }

                    if (readSize < dataSize) {
                        byte[] newBuffer = new byte[readSize];
                        System.arraycopy(buffer, 0, newBuffer, 0, readSize);
                        buffer = newBuffer;
                    }

                    readOffset += readSize;
                    if (isEOF) {
                        size = readOffset;
                    }
                    break;
                } else if (readOffset < dataOffset) {
                    readOffset += inputStream.skip(dataOffset - readOffset);
                } else {
                    throw new IOException("read stream data error");
                }
            }
        }
        return buffer;
    }

    @Override
    public void close() {
    }

    @Override
    String getSourceType() {
        return hasSize ? "Stream:HasSize" : "Stream:NoSize";
    }
}
