package com.danifoldi.forest.seed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageProvider {

    private static final Map<String, String> messageCache = new ConcurrentHashMap<>();

    public static String provide(String key) {
        return messageCache.getOrDefault(key, "?");
    }

    public static void update(Map<String, String> values) {
        messageCache.clear();
        messageCache.putAll(values);
    }
}
