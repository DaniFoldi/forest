package com.danifoldi.forest.tree.cron;

import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.forest.tree.dataverse.DataverseTree;
import com.danifoldi.forest.tree.task.TaskTree;
import com.danifoldi.microbase.BaseScheduler;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@VersionCollector("1.0.0")
@DependencyCollector(tree="config", minVersion="1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
@DependencyCollector(tree="task", minVersion="1.0.0")
public class CronTree implements Tree {

    private static BaseScheduler.BaseTask cronTask;

    private static final CronDefinition DEFINITION = CronDefinitionBuilder.defineCron()
            .withMinutes().withValidRange(0, 59).withStrictRange().and()
            .withHours().withValidRange(0, 23).withStrictRange().and()
            .withDayOfMonth().withValidRange(1, 31).withStrictRange().and()
            .withMonth().withValidRange(1, 12).withStrictRange().and()
            .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0).withStrictRange().and()
            .withSupportedNicknameHourly()
            .withSupportedNicknameDaily()
            .withSupportedNicknameMidnight()
            .withSupportedNicknameWeekly()
            .withSupportedNicknameMonthly()
            .withSupportedNicknameAnnually()
            .withSupportedNicknameYearly()
            .instance();
    private static final CronParser PARSER = new CronParser(DEFINITION);

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            NamespacedMultiDataVerse<CronTask> cronDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(GrownTrees.get(DataverseTree.class), "cron_%s".formatted(TreeLoader.getServerId()), CronTask::new);
            CronConfig config = GrownTrees.get(ConfigTree.class).getConfigFor("cron", true, CronConfig::new).join();
            if (config.catchupOnStartup) {
                Microbase.getScheduler().runTaskAfter(() -> {
                    CompletableFuture.allOf(cronDataverse.list().join().stream().map(pair ->
                            CompletableFuture.runAsync(() -> {
                                Optional<ZonedDateTime> next = ExecutionTime.forCron(PARSER.parse(pair.getFirst())).nextExecution(pair.getSecond().lastRun.atZone(ZoneId.systemDefault()));
                                if (config.catchupMultipleTimes) {
                                    while (next.isPresent() && next.get().isBefore(Instant.now().atZone(ZoneId.systemDefault()))) {
                                        cronDataverse.delete(pair.getFirst(), pair.getSecond()).join();
                                        pair.getSecond().lastRun = next.orElse(Instant.now().atZone(ZoneId.systemDefault())).toInstant();
                                        cronDataverse.add(pair.getFirst(), pair.getSecond()).join();
                                        next = ExecutionTime.forCron(PARSER.parse(pair.getFirst())).nextExecution(pair.getSecond().lastRun.atZone(ZoneId.systemDefault()));
                                    }
                                } else {
                                    if (next.isPresent() && next.get().isBefore(Instant.now().atZone(ZoneId.systemDefault()))) {
                                        cronDataverse.delete(pair.getFirst(), pair.getSecond()).join();
                                        pair.getSecond().lastRun = ExecutionTime.forCron(PARSER.parse(pair.getFirst())).lastExecution(Instant.now().atZone(ZoneId.systemDefault())).orElse(Instant.now().atZone(ZoneId.systemDefault())).toInstant();
                                        cronDataverse.add(pair.getFirst(), pair.getSecond()).join();
                                        GrownTrees.get(TaskTree.class).run(pair.getSecond());
                                    }
                                }
                    })).toArray(CompletableFuture[]::new)).join();
                }, config.catchupDelay, TimeUnit.MILLISECONDS);
            }

            if (cronTask != null) {
                cronTask.cancel();
            }

            cronTask = Microbase.getScheduler().runTaskEvery(() ->
                    CompletableFuture.allOf(cronDataverse.list().join().stream().map(pair ->
                    CompletableFuture.runAsync(() -> {
                Optional<ZonedDateTime> next = ExecutionTime.forCron(PARSER.parse(pair.getFirst())).nextExecution(pair.getSecond().lastRun.atZone(ZoneId.systemDefault()));
                if (next.isPresent() && next.get().isBefore(Instant.now().atZone(ZoneId.systemDefault()))) {
                    cronDataverse.delete(pair.getFirst(), pair.getSecond()).join();
                    pair.getSecond().lastRun = next.get().toInstant();
                    cronDataverse.add(pair.getFirst(), pair.getSecond()).join();
                    GrownTrees.get(TaskTree.class).run(pair.getSecond());
                }
            })).toArray(CompletableFuture[]::new)).join(), 20, TimeUnit.SECONDS);
        }, Microbase.getThreadPool("cron"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            if (cronTask != null) {
                cronTask.cancel();
            }
            return Microbase.shutdownThreadPool("cron", 1000, force);
        });
    }
}
