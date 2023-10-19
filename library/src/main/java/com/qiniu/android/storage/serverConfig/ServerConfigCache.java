package com.qiniu.android.storage.serverConfig;

import com.qiniu.android.storage.Recorder;
import com.qiniu.android.utils.Cache;

class ServerConfigCache {

    private static final String kServerConfigDiskKey = "ServerConfig";
    private static final String kServerUserConfigDiskKey = "ServerUserConfig";

    private final Cache configCache = new Cache.Builder(ServerConfig.class)
            .setVersion("v1.0.0")
            .builder();
    private final Cache userConfigCache = new Cache.Builder(ServerUserConfig.class)
            .setVersion("v1.0.0")
            .builder();
    ;

    ServerConfigCache() {
    }

    ServerConfig getConfig() {
        Cache.Object object = this.configCache.cacheForKey(kServerConfigDiskKey);
        if (object instanceof ServerConfig) {
            return (ServerConfig) object;
        }
        return null;
    }

    void setConfig(ServerConfig config) {
        this.configCache.cache(kServerConfigDiskKey, config, true);
    }

    ServerUserConfig getUserConfig() {
        Cache.Object object = this.userConfigCache.cacheForKey(kServerUserConfigDiskKey);
        if (object instanceof ServerUserConfig) {
            return (ServerUserConfig) object;
        }
        return null;
    }

    void setUserConfig(ServerUserConfig userConfig) {
        this.userConfigCache.cache(kServerUserConfigDiskKey, userConfig, true);
    }

    public synchronized void removeConfigCache() {
        this.configCache.clearDiskCache();
        this.configCache.clearMemoryCache();
        this.userConfigCache.clearDiskCache();
        this.userConfigCache.clearMemoryCache();
    }
}
