package com.danifoldi.forest.tree.dataverse;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@VersionCollector("1.0.0")
public class DataverseTree implements Tree {

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlColumn("java.util.regex.Pattern", "VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlQuery("java.util.regex.Pattern", (statement, i, spec, obj) -> statement.setString(i, ((Pattern)spec.reflect().get(obj)).pattern()));
            DataVerse.getDataVerse().getTranslationEngine().addMysqlResultToJavaType("java.util.regex.Pattern", (results, colName, spec, obj) -> spec.reflect().set(obj, Pattern.compile(results.getString(colName))));

            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlColumn("java.time.Instant", "VARCHAR(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlQuery("java.time.Instant", (statement, i, spec, obj) -> statement.setString(i, ((Instant)spec.reflect().get(obj)).toString()));
            DataVerse.getDataVerse().getTranslationEngine().addMysqlResultToJavaType("java.time.Instant", (results, colName, spec, obj) -> spec.reflect().set(obj, Instant.parse(results.getString(colName))));

            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlColumn("java.lang.Throwable", "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            DataVerse.getDataVerse().getTranslationEngine().addJavaTypeToMysqlQuery("java.lang.Throwable", (statement, i, spec, obj) -> {
                Throwable throwable = (Throwable)spec.reflect().get(obj);
                String details = "%s____%s".formatted(throwable.getMessage(), Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString));
                statement.setString(i, details);
            });

            // NOTE stack trace is not rebuilt
            DataVerse.getDataVerse().getTranslationEngine().addMysqlResultToJavaType("java.lang.Throwable", (results, colName, spec, obj) -> spec.reflect().set(obj, new Throwable(results.getString(colName))));
        }, Microbase.getThreadPool("dataverse"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("dataverse", 1000, force));
    }
}
