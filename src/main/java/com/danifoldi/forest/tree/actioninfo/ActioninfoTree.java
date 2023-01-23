package com.danifoldi.forest.tree.actioninfo;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.listener.ForestLeaveEvent;
import com.danifoldi.forest.tree.listener.ListenerTree;
import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.BaseScheduler;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.Pair;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@VersionCollector("1.0.0")
@DependencyCollector(tree="listener", minVersion="1.0.0")
public class ActioninfoTree implements Tree {

    private Multimap<UUID, Pair<BaseMessage, BaseMessage>> items = MultimapBuilder.hashKeys().linkedHashSetValues().build();

    private BiMap<Pair<UUID, String>, Pair<BaseMessage, BaseMessage>> pairs = HashBiMap.create();
    private BaseScheduler.BaseTask actionbarTask;

    @MessageCollector(value="actioninfo.separator.long", replacements={"rest", "last"})
    @MessageCollector(value="actioninfo.separator.short", replacements={"rest", "last"})
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            actionbarTask = Microbase.getScheduler().runTaskEvery(() -> {
                items.keySet().forEach(uuid -> {
                    Collection<Pair<BaseMessage, BaseMessage>> actions = items.get(uuid);
                    if (actions.size() == 0) {
                        return;
                    }

                    BasePlayer player = Microbase.getPlatform().getPlayer(uuid);
                    if (player == null) {
                        items.removeAll(uuid);
                        return;
                    }

                    AtomicReference<BaseMessage> message = new AtomicReference<>(null);
                    Function<Pair<BaseMessage, BaseMessage>, BaseMessage> getter = Pair::getFirst;
                    if (actions.size() > 3) {
                        getter = Pair::getSecond;
                    }

                    actions.stream().map(getter).forEach(m -> {
                        if (message.get() == null) {
                          message.set(m);
                        } else {
                            message.set(message.get().text().rawText(Microbase.provideMessage("actioninfo.separator." + (actions.size() > 3 ? "long" : "short"))).text().append(m));
                        }
                    });
                    player.actionbar(message.get());
                });
            }, 500, TimeUnit.MILLISECONDS);
            GrownTrees.get(ListenerTree.class).addListener(ActioninfoTree.class, ForestLeaveEvent.class, event -> {
                UUID uuid = event.player().uniqueId();
                Collection<Pair<BaseMessage, BaseMessage>> actions = items.removeAll(uuid);
                for (Pair<BaseMessage, BaseMessage> pair: actions) {
                    pairs.inverse().remove(pair);
                }
            });
        }, Microbase.getThreadPool("actioninfo"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            actionbarTask.cancel();
            return Microbase.shutdownThreadPool("actioninfo", 1000, force);
        });
    }

    public UUID addAction(BasePlayer player, String key, BaseMessage shortMessage, BaseMessage longMessage) {
        UUID uuid = UUID.randomUUID();
        Pair<BaseMessage, BaseMessage> message = Pair.of(shortMessage, longMessage);
        Pair<UUID, String> id = Pair.of(player.uniqueId(), key);
        if (pairs.containsKey(id)) {
            items.remove(player.uniqueId(), pairs.remove(id));
        }
        items.put(player.uniqueId(), message);
        pairs.put(Pair.of(player.uniqueId(), key), message);
        return uuid;
    }

    public void removeAction(BasePlayer player, String key) {
        Pair<BaseMessage, BaseMessage> item = pairs.remove(Pair.of(player.uniqueId(), key));
        if (item == null) {
            return;
        }
        items.remove(player.uniqueId(), item);
    }
}
