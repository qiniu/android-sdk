package com.qiniu.android.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by bailong on 16/9/7.
 */
public final class ContextGetter {
    public static Context applicationContext() {
        try {
            Application app = getApplicationUsingReflection();
            return app.getApplicationContext();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Application getApplicationUsingReflection() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null, (Object[]) null);
    }
}
