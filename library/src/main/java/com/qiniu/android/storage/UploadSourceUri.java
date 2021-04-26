package com.qiniu.android.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class UploadSourceUri extends UploadSourceStream {

    private final Uri uri;
    private String modifyDate = "";

    UploadSourceUri(Uri uri) {
        super(createInputStream(uri));
        this.uri = uri;
        loadFileInfo();
        reloadInfo();
    }

    @Override
    public String getId() {
        return getFileName() + modifyDate;
    }

    @Override
    public boolean couldReloadInfo() {
        return uri != null && StringUtils.isNullOrEmpty(uri.getScheme());
    }

    @Override
    public boolean reloadInfo() {
        InputStream inputStream = createInputStream(uri);
        setInputStream(inputStream);
        return inputStream != null;
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

    private static InputStream createInputStream(Uri uri) {
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
        } catch (FileNotFoundException ignore) {
        }
        return inputStream;
    }

    private void loadFileInfo() {
        if (uri == null) {
            return;
        }

        ContentResolver resolver = getContextResolver();
        if (resolver == null) {
            return;
        }

        Cursor cursor = resolver.query(uri, null, null, null, null, null);
        if (cursor == null) {
            return;
        }

        try {
            if (cursor.moveToFirst()) {
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

    private static ContentResolver getContextResolver() {
        Context context = ContextGetter.applicationContext();
        if (context == null || context.getContentResolver() == null) {
            return null;
        }
        return context.getContentResolver();
    }
}
