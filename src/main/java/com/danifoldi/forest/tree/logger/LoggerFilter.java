package com.danifoldi.forest.tree.logger;

import java.util.regex.Pattern;

public class LoggerFilter {
    int minLevel;
    Pattern classFilter;
    Pattern methodFilter;
    Pattern messageFilter;
    Pattern loggerFilter;
    Pattern bundleFilter;
}
