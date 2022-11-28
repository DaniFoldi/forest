package com.danifoldi.forest.tree.task;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.forest.tree.listener.ForestJoinEvent;
import com.danifoldi.forest.tree.listener.ListenerTree;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@VersionCollector("1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
public class TaskTree implements Tree {

    NamespacedMultiDataVerse<Task> taskDataverse;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            taskDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(DataverseNamespace.get(), "task_queue", Task::new);
            GrownTrees.get(ListenerTree.class).addListener(TaskTree.class, ForestJoinEvent.class, event -> {
                taskDataverse.get(event.playerName().toLowerCase(Locale.ROOT)).thenAcceptAsync(tasks -> {
                    for (Task task: tasks) {
                        run(task);
                        taskDataverse.delete(event.playerName().toLowerCase(Locale.ROOT), task);
                    }
                }, Microbase.getThreadPool("task"));
            });
        }, Microbase.getThreadPool("task"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("task", 1000, force));
    }


    public void run(Task task) {
        if (task.type.startsWith("console")) {
            Pattern pattern = Pattern.compile("console(\\((?<player>\\w+)\\))?(?<delay>\\+\\d+)?(?<save>!)?");
            Matcher matcher = pattern.matcher(task.type);
            if (!matcher.matches()) {
                Microbase.logger.log(Level.WARNING, "Failed to parse console task type %s".formatted(task.type));
                return;
            }
            int delay = Integer.parseInt(Optional.ofNullable(matcher.group("delay")).orElse("0"));
            Optional<String> player = Optional.ofNullable(matcher.group("player"));

            if (player.isPresent() && !player.get().isEmpty()) {
                Microbase.getScheduler().runTaskAfter(() -> {
                    BasePlayer basePlayer = Microbase.getPlatform().getPlayer(player.get());
                    if (basePlayer != null) {
                        Microbase.getPlatform().runConsoleCommand(task.value);
                    } else if (matcher.group("save") != null) {
                        taskDataverse.add(player.get().toLowerCase(Locale.ROOT), task);
                    }
                }, delay, TimeUnit.SECONDS);
            } else {
                Microbase.getScheduler().runTaskAfter(() -> Microbase.getPlatform().runConsoleCommand(task.value), delay, TimeUnit.SECONDS);
            }
        } else if (task.type.startsWith("player")) {
            Pattern pattern = Pattern.compile("console\\((?<player>\\w+)\\)(?<delay>\\+\\d+)?");
            Matcher matcher = pattern.matcher(task.type);
            if (!matcher.matches()) {
                Microbase.logger.log(Level.WARNING, "Failed to parse console task type %s".formatted(task.type));
                return;
            }
            int delay = Integer.parseInt(Optional.ofNullable(matcher.group("delay")).orElse("0"));
            Optional<String> player = Optional.ofNullable(matcher.group("player"));

            if (player.isPresent() && !player.get().isEmpty()) {
                Microbase.getScheduler().runTaskAfter(() -> {
                    BasePlayer basePlayer = Microbase.getPlatform().getPlayer(player.get());
                    if (basePlayer != null) {
                        basePlayer.run(task.value);
                    } else if (matcher.group("save") != null) {
                        taskDataverse.add(player.get().toLowerCase(Locale.ROOT), task);
                    }
                }, delay, TimeUnit.SECONDS);
            } else {
                Microbase.logger.log(Level.WARNING, "Task %s failed to parse player".formatted(task.type));
            }
        } else {
            Microbase.logger.log(Level.WARNING, "Unknown task type %s value %s".formatted(task.type, task.value));
        }
    }
}
