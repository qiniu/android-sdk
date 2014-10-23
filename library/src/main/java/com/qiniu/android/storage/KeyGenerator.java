package com.qiniu.android.storage;

import java.io.File;

/**
 * Created by bailong on 14/10/21.
 */
public interface KeyGenerator {
    String gen(String key, File file);
}
