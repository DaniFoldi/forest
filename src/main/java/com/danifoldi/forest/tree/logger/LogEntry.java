package com.danifoldi.forest.tree.logger;

import java.time.Instant;

public class LogEntry {
    int level;
    long sequenceNumber;
    String sourceClass;
    String sourceMethod;
    String message;
    long threadId;
    String logger;
    String resourceBundle;
    Instant time;
    Throwable throwable;

    public LogEntry() {

    }

    public LogEntry(int level,
                    long sequenceNumber,
                    String sourceClass,
                    String sourceMethod,
                    String message,
                    long threadId,
                    String logger,
                    String resourceBundle,
                    Instant time,
                    Throwable throwable) {
        this.level = level;
        this.sequenceNumber = sequenceNumber;
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
        this.message = message;
        this.threadId = threadId;
        this.logger = logger;
        this.resourceBundle = resourceBundle;
        this.time = time;
        this.throwable = throwable;
    }
}
