package com.danifoldi.forest.seed.collector.collector;

import java.lang.annotation.Repeatable;

@Repeatable(MessageCollector.MultiMessageCollector.class)
public @interface MessageCollector {
    String value();
    String[] replacements() default "";

    @interface MultiMessageCollector {
        MessageCollector[] value();
    }
}
