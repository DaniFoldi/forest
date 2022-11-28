package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable(PermissionCollector.MultiPermissionCollector.class)
public @interface PermissionCollector {
    String value();

    @interface MultiPermissionCollector {
        PermissionCollector[] value();
    }
}
