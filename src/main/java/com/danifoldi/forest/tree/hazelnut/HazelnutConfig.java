package com.danifoldi.forest.tree.hazelnut;

public class HazelnutConfig {
    public long cacheExpiryRate = hazelnut.core.config.HazelnutConfig.DEFAULT_CACHE_EXPIRY_RATE;
    public long cacheHousekeeperRate = hazelnut.core.config.HazelnutConfig.DEFAULT_CACHE_HOUSEKEEPER_RATE;
    public long heartbeatRate = hazelnut.core.config.HazelnutConfig.DEFAULT_HEARTBEAT_RATE;
    public String redisHost = "";
    public String redisUsername = "";
    public String redisPassword = "";
}
