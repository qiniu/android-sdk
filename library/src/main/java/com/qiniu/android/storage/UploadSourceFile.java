package com.qiniu.android.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class UploadSourceFile extends UploadSource {

    private Exception readException = null;
    private final File file;
    private final RandomAccessFile randomAccessFile;

    UploadSourceFile(File file) {
        this.file = file;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (Exception e) {
            readException = e;
        }
        this.randomAccessFile = randomAccessFile;
    }

    @Override
    public String getId() {
        return getFileName() + "_" + file.lastModified();
    }

    @Override
    public boolean couldReloadSource() {
        return randomAccessFile != null;
    }

    @Override
    public boolean reloadSource() {
        return true;
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (randomAccessFile == null) {
            if (readException != null) {
                throw new IOException(readException);
            } else {
                throw new IOException("file is invalid");
            }
        }

        int readSize = 0;
        byte[] buffer = new byte[dataSize];
        try {
            randomAccessFile.seek(dataOffset);
            while (readSize < dataSize) {
                int ret = randomAccessFile.read(buffer, readSize, (dataSize - readSize));
                if (ret < 0) {
                    break;
                }
                readSize += ret;
            }

            if (readSize < dataSize) {
                byte[] newBuffer = new byte[readSize];
                System.arraycopy(buffer, 0, newBuffer, 0, readSize);
                buffer = newBuffer;
            }
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
        return buffer;
    }

    @Override
    public void close() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                try {
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    String getSourceType() {
        return "File";
    }
}
