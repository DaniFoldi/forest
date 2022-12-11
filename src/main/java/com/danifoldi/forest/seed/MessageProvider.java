package com.danifoldi.forest.seed;

import com.danifoldi.microbase.Microbase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MessageProvider {

    private static final Map<String, String> messageCache = new ConcurrentHashMap<>();

    public static String provide(String key) {
        if (!messageCache.containsKey(key)) {
            messageCache.put(key, key);
            Microbase.logger.log(Level.WARNING, "Key %s is missing from providable messages".formatted(key));
        }
        return messageCache.get(key);
    }

    public static void update(Map<String, String> values) {
        messageCache.clear();
        messageCache.putAll(values);
    }
}
