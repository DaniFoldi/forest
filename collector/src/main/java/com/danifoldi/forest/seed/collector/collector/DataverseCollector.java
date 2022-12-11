package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface DataverseCollector {
    boolean multi();

    String dataverse();
}
