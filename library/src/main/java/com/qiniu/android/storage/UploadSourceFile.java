package com.qiniu.android.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

class UploadSourceFile implements UploadSource {

    private final File file;
    private final RandomAccessFile randomAccessFile;

    UploadSourceFile(File file) {
        this.file = file;
        RandomAccessFile randomAccessFile = null;
        if (file != null) {
            try {
                randomAccessFile = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException ignored) {
            }
        }
        this.randomAccessFile = randomAccessFile;
    }

    @Override
    public String getId() {
        return file.lastModified() + "";
    }

    @Override
    public boolean isValid() {
        return file != null && file.exists();
    }

    @Override
    public boolean couldReloadInfo() {
        return file != null;
    }

    @Override
    public boolean reloadInfo() {
        return false;
    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public long getFileSize() {
        return file.length();
    }

    @Override
    public byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (randomAccessFile == null) {
            return null;
        }

        int readSize = 0;
        byte[] data = new byte[dataSize];
        try {
            randomAccessFile.seek(dataOffset);
            while (readSize < dataSize) {
                int ret = randomAccessFile.read(data, readSize, (dataSize - readSize));
                if (ret < 0) {
                    break;
                }
                readSize += ret;
            }

            // 读数据非预期
            if (readSize != dataSize) {
                data = null;
            }
        } catch (IOException e) {
            data = null;
        }
        return data;
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
}
