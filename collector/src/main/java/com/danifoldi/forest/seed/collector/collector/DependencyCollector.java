package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable(DependencyCollector.MultiDependencyCollector.class)
public @interface DependencyCollector {
    String tree();
    String minVersion();

    @interface MultiDependencyCollector {
        DependencyCollector[] value();
    }
}
