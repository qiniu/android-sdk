package com.qiniu.android.collect;

import com.qiniu.android.common.Constants;
import com.qiniu.android.http.UserAgent;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.ContextGetter;
import com.qiniu.android.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UploadInfoElementCollector {

    public static LogHandler getUplogHandler(final Object obj) {
        final String setMethod = "set";
        LogHandler logHandler = new LogHandler() {
            @Override
            public void send(String key, Object value) {
                try {
                    if (value instanceof String) {
                        Method setByKey = obj.getClass().getMethod(setMethod + StringUtils.upperCase(key), Class.forName("java.lang.String"));
                        setByKey.invoke(obj, value);
                    } else if (value instanceof Integer) {
                        Method setByKey = obj.getClass().getMethod(setMethod + StringUtils.upperCase(key), int.class);
                        setByKey.invoke(obj, value);
                    } else if (value instanceof Long) {
                        Method setByKey = obj.getClass().getMethod(setMethod + StringUtils.upperCase(key), long.class);
                        setByKey.invoke(obj, value);
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    //忽略异常，构造时用默认值
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public Object getUploadInfo() {
                return obj;
            }
        };
        return logHandler;
    }


    public static void setReqCommonElements(UploadInfoElement.ReqInfo reqInfoQuery) {
        //os_version
        reqInfoQuery.setOs_version(UserAgent.osVersion());
        //sdk_version
        reqInfoQuery.setSdk_version(Constants.VERSION);
        //up_time
        reqInfoQuery.setUp_time(System.currentTimeMillis() / 1000);
        //network_type
        String network_type = AndroidNetwork.networkType(ContextGetter.applicationContext());
        reqInfoQuery.setNetwork_type(network_type);
        //signal_strength
        reqInfoQuery.setSignal_strength(AndroidNetwork.getMobileDbm());
    }

}
