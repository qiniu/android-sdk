package com.qiniu.android.storage;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@TargetApi(Build.VERSION_CODES.KITKAT)
class UploadSourceUri extends UploadSourceStream {

    private Exception readException = null;

    private final Uri uri;
    private ContentResolver resolver;
    private String modifyDate = "";

    UploadSourceUri(Uri uri, ContentResolver resolver) {
        super(null);
        this.uri = uri;
        this.resolver = resolver;

        reloadSource();
        loadFileInfo();
    }

    @Override
    public String getId() {
        return getFileName() + "_" + modifyDate;
    }

    @Override
    public boolean couldReloadSource() {
        return uri != null && !StringUtils.isNullOrEmpty(uri.getScheme());
    }

    @Override
    public boolean reloadSource() {
        super.reloadSource();
        close();
        readException = null;

        InputStream inputStream = null;
        try {
            inputStream = createInputStream();
            setInputStream(inputStream);
        } catch (Exception e) {
            readException = e;
        }
        return inputStream != null;
    }

    @Override
    public byte[] readData(int dataSize, long dataOffset) throws IOException {
        if (readException != null) {
            throw new IOException("Uri read data exception: " + readException, readException);
        }

        return super.readData(dataSize, dataOffset);
    }

    @Override
    public void close() {
        InputStream inputStream = getInputStream();
        if (inputStream != null) {
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

    private InputStream createInputStream() throws Exception {
        if (uri == null) {
            return null;
        }

        ContentResolver resolver = getContextResolver();
        if (resolver == null) {
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = resolver.openInputStream(uri);
        } catch (Exception e) {
            throw e;
        }
        return inputStream;
    }

    private void loadFileInfo() {
        if (uri == null) {
            return;
        }

        if ("file".equals(uri.getScheme())) {
            tryLoadFileInfoByPath();
        } else {
            tryLoadFileInfoByCursor();
        }
    }

    private void tryLoadFileInfoByPath() {
        if (uri.getPath() != null) {
            File file = new File(uri.getPath());
            if (file.exists() && file.isFile()) {
                setFileName(file.getName());
                setSize(file.length());
                modifyDate = file.lastModified() + "";
            }
        }
    }

    private void tryLoadFileInfoByCursor() {
        ContentResolver resolver = getContextResolver();
        if (resolver == null) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cursor == null) {
            return;
        }

        try {
            if (cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (!cursor.isNull(dataIndex)) {
                    String path = cursor.getString(dataIndex);
                    if (path != null) {
                        File file = new File(path);
                        setSize(file.length());
                        setFileName(file.getName());
                        modifyDate = file.lastModified() / 1000 + "";
                        return;
                    }
                }

                int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    long size = cursor.getLong(sizeIndex);
                    setSize(size);
                }

                int fileNameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                if (!cursor.isNull(fileNameIndex)) {
                    setFileName(cursor.getString(fileNameIndex));
                }

                int modifyDateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);
                if (!cursor.isNull(modifyDateIndex)) {
                    modifyDate = cursor.getString(modifyDateIndex);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private ContentResolver getContextResolver() {
        if (resolver != null) {
            return resolver;
        }

        Context context = ContextGetter.applicationContext();
        if (context != null) {
            resolver = context.getContentResolver();
        }

        return resolver;
    }

    @Override
    String getSourceType() {
        return "Uri";
    }
}
