package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.storage.FileRecorder;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

class ServerConfigCache {

    private ServerConfig config;
    private ServerUserConfig userConfig;

    private static String kServerConfigDiskKey = "ServerConfig:v1.0.0";
    private static String kServerUserConfigDiskKey = "ServerUserConfig:v1.0.0";

    private Recorder recorder;


    synchronized ServerConfig getConfig() {
        return config;
    }

    synchronized void setConfig(ServerConfig config) {
        this.config = config;
    }

    ServerConfig getConfigFromDisk() {

        byte[] configData = null;
        synchronized (this) {
            setupRecorder();
            configData = recorder.get(kServerConfigDiskKey);
        }
        if (configData == null) {
            return null;
        }

        JSONObject configJson = null;
        try {
            configJson = new JSONObject(new String(configData));
        } catch (Exception ignored) {
            synchronized (this) {
                recorder.del(kServerConfigDiskKey);
            }
            return null;
        }
        return new ServerConfig(configJson);
    }

    void saveConfigToDisk(ServerConfig config) {
        if (config == null || config.getInfo() == null) {
            return;
        }

        synchronized (this) {
            setupRecorder();
            recorder.set(kServerConfigDiskKey, config.getInfo().toString().getBytes());
        }
    }


    synchronized ServerUserConfig getUserConfig() {
        return userConfig;
    }

    synchronized void setUserConfig(ServerUserConfig userConfig) {
        this.userConfig = userConfig;
    }

    ServerUserConfig getUserConfigFromDisk() {

        byte[] configData = null;
        synchronized (this) {
            setupRecorder();
            configData = recorder.get(kServerUserConfigDiskKey);
        }
        if (configData == null) {
            return null;
        }

        JSONObject configJson = null;
        try {
            configJson = new JSONObject(new String(configData));
        } catch (Exception ignored) {
            synchronized (this) {
                recorder.del(kServerUserConfigDiskKey);
            }
            return null;
        }
        return new ServerUserConfig(configJson);
    }

    void saveUserConfigToDisk(ServerUserConfig userConfig) {
        if (userConfig == null || userConfig.getInfo() == null) {
            return;
        }

        synchronized (this) {
            setupRecorder();
            recorder.set(kServerUserConfigDiskKey, userConfig.getInfo().toString().getBytes());
        }
    }

    public synchronized void removeConfigCache() {
        setupRecorder();
        setConfig(null);
        setUserConfig(null);
        recorder.del(kServerConfigDiskKey);
        recorder.del(kServerUserConfigDiskKey);
    }

    private void setupRecorder() {
        if (recorder == null) {
            try {
                recorder = new FileRecorder(Utils.sdkDirectory() + "/ServerConfig");
            } catch (Exception ignored) {
            }
        }
    }
}
