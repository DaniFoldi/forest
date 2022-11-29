package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable(PlatformCollector.MultiPlatformCollector.class)
public @interface PlatformCollector {
    String value();

    @interface MultiPlatformCollector {
        PlatformCollector[] value();
    }
}
