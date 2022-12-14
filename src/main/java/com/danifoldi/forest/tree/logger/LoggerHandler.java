package com.danifoldi.forest.tree.logger;

import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LoggerHandler extends Handler {

    NamespacedMultiDataVerse<LogEntry> logDataverse;
    List<LoggerFilter> filters;

    List<Consumer<LogEntry>> handlers = Collections.synchronizedList(new ArrayList<>());

    public LoggerHandler(NamespacedMultiDataVerse<LogEntry> logDataverse, List<LoggerFilter> filters) {
        this.logDataverse = logDataverse;
        this.filters = filters;
    }

    @Override
    public void publish(LogRecord record) {
        CompletableFuture.runAsync(() -> {
            for (LoggerFilter filter: filters) {
                if (record.getLevel().intValue() < filter.minLevel && (
                        !filter.classFilter.asMatchPredicate().test(record.getSourceClassName()) &&
                                !filter.methodFilter.asMatchPredicate().test(record.getSourceMethodName()) &&
                                !filter.messageFilter.asMatchPredicate().test(record.getMessage()) &&
                                !filter.loggerFilter.asMatchPredicate().test(record.getLoggerName()) &&
                                !filter.bundleFilter.asMatchPredicate().test(record.getResourceBundleName())
                )) {
                    return;
                }
            }
            LogEntry entry = new LogEntry(
                    record.getLevel().intValue(),
                    record.getSequenceNumber(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getMessage(),
                    record.getLongThreadID(),
                    record.getLoggerName(),
                    record.getResourceBundleName(),
                    record.getInstant(),
                    record.getThrown()
            );

            handlers.forEach(handler -> handler.accept(entry));
            logDataverse.add(TreeLoader.getServerId(), entry).join();
        }, Microbase.getThreadPool("logger"));
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
