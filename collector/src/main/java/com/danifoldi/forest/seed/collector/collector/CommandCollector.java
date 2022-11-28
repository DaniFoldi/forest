package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable(value = CommandCollector.MultiCommandCollector.class)
public @interface CommandCollector {
    String value();

    @interface MultiCommandCollector {
        CommandCollector[] value();
    }
}
