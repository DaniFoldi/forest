package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface ConfigCollector {
    boolean global();

    String set();
}
