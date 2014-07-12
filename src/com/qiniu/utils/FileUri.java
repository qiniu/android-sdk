package com.qiniu.utils;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

public class FileUri {
    public static File getFile(Context mContext, Uri uri) {
        uri = fileUri(mContext, uri);
        return new File(uri.getEncodedPath());
    }

    private static Uri fileUri(Context mContext, Uri uri){
        if (uri.toString().startsWith("file")){
            return uri;
        }
        String filePath;
        if (uri != null && "content".equals(uri.getScheme())) {
            Cursor cursor = mContext.getContentResolver().query(uri,
                new String[] { android.provider.MediaStore.Images.ImageColumns.DATA },
                null, null, null);
            cursor.moveToFirst();
            filePath = cursor.getString(0);
            cursor.close();
        } else {
            filePath = uri.getPath();
        }
        return Uri.parse("file://" + filePath);
    }

    public static File getSDPath(Context context){
        File sdDir = context.getCacheDir();
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        return sdDir;
    }
}
