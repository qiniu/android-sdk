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

    private boolean isHandlingServerConfigDisk = false;
    private boolean isHandlingServerUserConfigDisk = false;
    private Recorder recorder;


    ServerConfig getConfig() {
        if (config == null) {
            config = getConfigFromDish();
        }
        return config;
    }

    void setConfig(ServerConfig config) {
        this.config = config;
        setConfigToDisk(config);
    }

    private ServerConfig getConfigFromDish() {
        setupRecorder();

        synchronized (this) {
            isHandlingServerConfigDisk = true;
        }
        byte[] configData = recorder.get(kServerConfigDiskKey);
        synchronized (this) {
            isHandlingServerConfigDisk = false;
        }

        if (configData == null) {
            return null;
        }

        JSONObject configJson = null;
        try {
            configJson = new JSONObject(new String(configData));
        } catch (Exception ignored) {
            return null;
        }
        return new ServerConfig(configJson);
    }

    private void setConfigToDisk(ServerConfig config) {
        if (config == null || config.getInfo() == null) {
            return;
        }

        synchronized (this) {
            if (isHandlingServerConfigDisk) {
                return;
            }
        }

        setupRecorder();
        recorder.set(kServerConfigDiskKey, config.getInfo().toString().getBytes());
    }


    ServerUserConfig getUserConfig() {
        if (userConfig == null) {
            userConfig = getUserConfigFromDisk();
        }
        return userConfig;
    }

    void setUserConfig(ServerUserConfig userConfig) {
        this.userConfig = userConfig;
        setUserConfigToDisk(userConfig);
    }

    private ServerUserConfig getUserConfigFromDisk() {
        setupRecorder();

        synchronized (this) {
            isHandlingServerUserConfigDisk = true;
        }
        byte[] configData = recorder.get(kServerUserConfigDiskKey);
        synchronized (this) {
            isHandlingServerUserConfigDisk = false;
        }

        if (configData == null) {
            return null;
        }

        JSONObject configJson = null;
        try {
            configJson = new JSONObject(new String(configData));
        } catch (Exception ignored) {
            return null;
        }
        return new ServerUserConfig(configJson);
    }

    private void setUserConfigToDisk(ServerUserConfig userConfig) {
        if (userConfig == null || userConfig.getInfo() == null) {
            return;
        }

        synchronized (this) {
            if (isHandlingServerUserConfigDisk) {
                return;
            }
        }

        setupRecorder();
        recorder.set(kServerUserConfigDiskKey, userConfig.getInfo().toString().getBytes());
    }

    private synchronized void setupRecorder() {
        if (recorder == null) {
            try {
                recorder = new FileRecorder(Utils.sdkDirectory() + "/ServerConfig");
            } catch (Exception ignored) {
            }
        }
    }
}
