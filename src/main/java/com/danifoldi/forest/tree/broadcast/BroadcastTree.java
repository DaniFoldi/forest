package com.danifoldi.forest.tree.broadcast;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.command.CommandTree;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.forest.tree.dataverse.DataverseTree;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.BaseScheduler;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.Pair;
import grapefruit.command.CommandContainer;
import grapefruit.command.CommandDefinition;
import grapefruit.command.parameter.modifier.Flag;
import grapefruit.command.parameter.modifier.Source;
import grapefruit.command.parameter.modifier.string.Greedy;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@VersionCollector("1.0.0")
@DependencyCollector(tree="command", minVersion="1.0.0")
@DependencyCollector(tree="config", minVersion="1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
public class BroadcastTree implements Tree, CommandContainer {

    private BaseScheduler.BaseTask broadcastTask;
    private NamespacedMultiDataVerse<BroadcastAnnouncement> announcementDataverse;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            GrownTrees.get(CommandTree.class).registerCommands(this);
            announcementDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(GrownTrees.get(DataverseTree.class), "broadcast_%s".formatted(TreeLoader.getServerId()), BroadcastAnnouncement::new);
            GrownTrees.get(ConfigTree.class).getConfigFor("broadcast", true, BroadcastConfig::new).thenAcceptAsync(config -> {
                broadcastTask = Microbase.getScheduler().runTaskEvery(() -> {
                    announcementDataverse.list(1, 1, announcementDataverse.getField("priority"), true).thenAcceptAsync(announcements -> {
                        Pair<String, BroadcastAnnouncement> announcement = announcements.get(0);
                        announcementDataverse.delete(announcement.getFirst(), announcement.getSecond());
                        broadcastImmediately(announcement.getSecond());
                        if (announcement.getSecond().remainingRepeatCount > 0) {
                            announcement.getSecond().remainingRepeatCount--;
                        }
                        if (announcement.getSecond().remainingRepeatCount != 0) {
                            announcementDataverse.add(announcement.getFirst(), announcement.getSecond());
                        }
                    }, Microbase.getThreadPool("broadcast"));
                }, config.broadcastDelay, TimeUnit.SECONDS, config.initialDelay);
            });
        }, Microbase.getThreadPool("broadcast"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(CommandTree.class).unregisterCommands(this);
            broadcastTask.cancel();
            return Microbase.shutdownThreadPool("broadcast", 1000, force);
        });
    }

    public void broadcastImmediately(BroadcastAnnouncement announcement) {
        List<String> servers = Arrays.asList(announcement.serverCondition.split(","));
        List<String> permissions = Arrays.asList(announcement.permissionCondition.split(","));
        List<String> players = Arrays.asList(announcement.playerCondition.split(","));
        List<String> worlds = Arrays.asList(announcement.worldCondition.split(","));
        for (BasePlayer player: Microbase.getPlatform().getPlayers()) {
            if (!servers.isEmpty() && !servers.contains(player.connectedTo().name())) {
                continue;
            }
            if (!permissions.isEmpty() && !permissions.stream().allMatch(player::hasPermission)) {
                return;
            }
            if (!players.isEmpty() && !players.contains(player.name())) {
                continue;
            }
            if (!worlds.isEmpty() && !worlds.contains(player.connectedTo().name())) {
                continue;
            }

            switch (announcement.mode) {
                case "chat" -> player.send(announcement.message);
                case "actionbar" -> player.actionbar(announcement.message);
                case "bossbar" -> Microbase.logger.log(Level.WARNING, "Bossbars are not yet supported");
                default -> Microbase.logger.log(Level.WARNING, "Mode %s is invalid for broadcast".formatted(announcement.mode));
            }
        }
        Microbase.logger.log(Level.INFO, announcement.message.toString());
    }

    public CompletableFuture<?> queueBroadcast(BroadcastAnnouncement announcement) {
        return CompletableFuture.runAsync(() -> announcementDataverse.add(UUID.randomUUID(), announcement));
    }

    @CommandCollector("broadcast")
    @PermissionCollector("forest.broadcast.command.broadcast")
    @CommandDefinition(route="broadcast", permission="forest.broadcast.command.broadcast", runAsync=true)
    public void onBroadcast(@Source BaseSender sender, @Flag(value="mode", shorthand='m') String mode, int priority, int repeat, @Greedy String message) {
        BroadcastAnnouncement announcement = new BroadcastAnnouncement();
        announcement.mode = mode;
        announcement.priority = priority;
        announcement.remainingRepeatCount = repeat;
        announcement.message = Microbase.baseMessage().colorizedText(message);
        queueBroadcast(announcement).join();
    }

    @CommandCollector("broadcastnow")
    @PermissionCollector("forest.broadcast.command.broadcastnow")
    @CommandDefinition(route="broadcastnow", permission="forest.broadcast.command.broadcastnow", runAsync=true)
    public void onBroadcast(@Source BaseSender sender, @Flag(value="mode", shorthand='m') String mode, @Greedy String message) {
        BroadcastAnnouncement announcement = new BroadcastAnnouncement();
        announcement.mode = mode;
        announcement.message = Microbase.baseMessage().colorizedText(message);
        broadcastImmediately(announcement);
    }
}
