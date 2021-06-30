package com.qiniu.android.utils;

import android.app.Application;
import android.content.Context;

import com.qiniu.android.storage.GlobalConfiguration;

/**
 * Created by bailong on 16/9/7.
 */
public final class ContextGetter {
    private static Context context = applicationContext();

    public static Context applicationContext() {
        if (context != null) {
            return context;
        }

        if (GlobalConfiguration.appContext != null) {
            return GlobalConfiguration.appContext;
        }

        Application app = getApplicationUsingReflection();
        if (app != null) {
            context = app.getApplicationContext();
        }
        return context;
    }

    private static Application getApplicationUsingReflection() {
        Application app = null;
        try {
            Class activity = Class.forName("android.app.ActivityThread");
            app = (Application) activity.getMethod("currentApplication").invoke(null, (Object[]) null);

            if (app == null) {
                Object localObject = activity.getMethod("currentActivityThread", new Class[0]).invoke(null, (Object[]) null);
                app = (Application) activity.getMethod("getApplication").invoke(localObject, (Object[]) null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return app;
    }
}
