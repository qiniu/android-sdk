package com.qiniu.android.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by bailong on 16/9/7.
 */
public final class ContextGetter {
    private static Application app = getApplicationUsingReflection();

    public static Context applicationContext() {
        if (app == null) {
            app = getApplicationUsingReflection();
        }

        return app;
    }

    private static Application getApplicationUsingReflection() {
        Application app = null;
        try {
            Class activity = Class.forName("android.app.ActivityThread");
            app = (Application) activity.getMethod("currentApplication").invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return app;
    }
}
