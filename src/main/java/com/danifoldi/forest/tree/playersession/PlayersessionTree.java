package com.danifoldi.forest.tree.playersession;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedDataVerse;
import com.danifoldi.dataverse.util.Pair;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.forest.tree.listener.ForestJoinEvent;
import com.danifoldi.forest.tree.listener.ForestLeaveEvent;
import com.danifoldi.forest.tree.listener.ListenerTree;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@VersionCollector("1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
@DependencyCollector(tree="listener", minVersion="1.0.0")
public class PlayersessionTree implements Tree {

    NamespacedDataVerse<PlayerSession> sessionDataverse;
    Map<String, UUID> sessionIds = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            sessionDataverse = DataVerse.getDataVerse().getNamespacedDataVerse(DataverseNamespace.get(), "playersession_%s".formatted(TreeLoader.getServerId()), PlayerSession::new);
            GrownTrees.get(ListenerTree.class).addListener(PlayersessionTree.class, ForestJoinEvent.class, event -> {
                CompletableFuture.runAsync(() -> {
                    if (sessionIds.containsKey(event.playerName())) {
                        Microbase.logger.log(Level.WARNING, "Player %s had an unfinished session".formatted(event.playerName()));
                        sessionIds.remove(event.playerName());
                    }
                    UUID sessionId = UUID.randomUUID();
                    int protocol = Microbase.getPlatform().getPlayer(event.playerName()).protocol();
                    sessionDataverse.create(sessionId, new PlayerSession(event.playerName(), event.uuid(), Instant.now(), event.ipAddress().toString(), event.hostname(), protocol));
                    sessionIds.put(event.playerName(), sessionId);
                }, Microbase.getThreadPool("playersession"));
            });
            GrownTrees.get(ListenerTree.class).addListener(PlayersessionTree.class, ForestLeaveEvent.class, event -> {
                CompletableFuture.runAsync(() -> {
                    if (!sessionIds.containsKey(event.player().name())) {
                        Microbase.logger.log(Level.WARNING, "Player %s didn't have a session id, skipping".formatted(event.player().name()));
                        return;
                    }
                    UUID sessionId = sessionIds.remove(event.player().name());
                    PlayerSession session = sessionDataverse.get(sessionId).join();
                    if (session == null) {
                        Microbase.logger.log(Level.WARNING, "Player %s didn't have a session saved".formatted(event.player().name()));
                        return;
                    }
                    session.left = Instant.now();
                    sessionDataverse.update(sessionId, session);
                }, Microbase.getThreadPool("playersession"));
            });
        }, Microbase.getThreadPool("playersession"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(ListenerTree.class).removeAllListeners(PlayersessionTree.class);
            return Microbase.shutdownThreadPool("playersession", 1000, force);
        });
    }

    public CompletableFuture<List<String>> recentlyLoggedOut(int count) {
        return CompletableFuture.supplyAsync(() -> {
            List<Pair<String, PlayerSession>> sessions = sessionDataverse.list(1, count, sessionDataverse.getField("left"), true).join();
            return sessions.stream().map(Pair::b).map(s -> s.playerName).toList();
        }, Microbase.getThreadPool("playersession"));
    }

    public CompletableFuture<Optional<UUID>> uuidOf(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            List<Pair<String, PlayerSession>> sessions = sessionDataverse.filterPrefix(sessionDataverse.getField("playerName"), playerName, 1, 100).join();
            for (PlayerSession session: sessions.stream().map(Pair::b).toList()) {
                if (session.playerName.equals(playerName)) {
                    return Optional.of(session.uuid);
                }
            }
            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0).b().uuid);
        }, Microbase.getThreadPool("playersession"));
    }
}
