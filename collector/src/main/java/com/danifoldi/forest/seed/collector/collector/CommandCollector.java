package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable()
public @interface CommandCollector {
    String value();
}
