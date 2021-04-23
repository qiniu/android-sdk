package com.qiniu.android.storage;

import android.content.Context;
import android.net.Uri;

import com.qiniu.android.utils.ContextGetter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class UploadSourceStream implements UploadSource {

    private long readOffset = 0;
    private Uri uri;
    private InputStream inputStream;
    private final String fileName;

    UploadSourceStream(Uri uri) {
        this.uri = uri;
        //todo: 获取文件名
        this.fileName = "";
        reloadInfo();
    }

    UploadSourceStream(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isValid() {
        return inputStream != null;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getFileSize() {
        return -1;
    }

    @Override
    public boolean couldReloadInfo() {
        return uri != null;
    }

    @Override
    public boolean reloadInfo() {
        if (!couldReloadInfo()) {
            return false;
        }

        Context context = ContextGetter.applicationContext();
        if (context == null || context.getContentResolver() == null) {
            return false;
        }

        try {
            inputStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    @Override
    public byte[] readData(int dataSize, long dataOffset) throws IOException {
        byte[] buffer = null;
        synchronized (this) {
            while (true) {
                if (readOffset == dataOffset) {
                    int readSize = 0;
                    buffer = new byte[dataSize];
                    while (readSize < dataSize) {
                        int ret = inputStream.read(buffer, readSize, dataSize - readSize);
                        if (ret < 0) {
                            break;
                        }
                        readSize += ret;
                    }
                    if (dataSize != readSize) {
                        byte[] newBuffer = new byte[readSize];
                        System.arraycopy(buffer, 0, newBuffer, 0, readSize);
                        buffer = newBuffer;
                    }
                    readOffset += readSize;
                    break;
                } else if (readOffset < dataOffset) {
                    readOffset += inputStream.skip(dataOffset - readOffset);
                } else {
                    throw new IOException("read block data error");
                }
            }
        }
        return buffer;
    }

    @Override
    public void close() {
        if (uri != null && inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
