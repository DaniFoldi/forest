package com.danifoldi.forest.tree.listener;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.microbase.Microbase;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

@VersionCollector("1.0.0")
public class ListenerTree implements Tree {

    private Object listener;

    private final Multimap<Class<? extends ForestEvent>, Consumer<ForestEvent>> eventHandlers = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Multimap<Class<? extends Tree>, Consumer<ForestEvent>> registeredHandlers = MultimapBuilder.hashKeys().linkedListValues().build();

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            this.listener = switch (Microbase.platformType) {
                case SPIGOT, PAPER -> new PaperListener(this);
                case BUNGEECORD, WATERFALL -> new WaterfallListener(this);
                case UNKNOWN -> {
                    Microbase.logger.log(Level.SEVERE, "Could not register listeners on unknown platform");
                    throw new RuntimeException("Could not register listeners on unknown platform");
                }
                case VELOCITY -> new VelocityListener(this);
            };
            Microbase.getPlatform().registerEventHandler(listener);
        }, Microbase.getThreadPool("listener"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.getPlatform().unregisterEventHandler(listener);
            eventHandlers.clear();
            return Microbase.shutdownThreadPool("listener", 1000, force);
        });
    }

    public <E extends ForestEvent> void addListener(Class<? extends Tree> tree, Class<E> type, Consumer<E> handler) {
        //noinspection unchecked
        eventHandlers.put(type, (Consumer<ForestEvent>)handler);
        //noinspection unchecked
        registeredHandlers.put(tree, (Consumer<ForestEvent>)handler);
    }


    public <E extends ForestEvent> void removeListener(Class<? extends Tree> tree, Class<E> type, Consumer<E> handler) {
        eventHandlers.remove(type, handler);
        registeredHandlers.remove(tree, handler);
    }

    public void removeAllListeners(Class<? extends Tree> tree) {
        for (Consumer<? extends ForestEvent> handler: registeredHandlers.get(tree)) {
            for (Class<? extends ForestEvent> type: eventHandlers.keySet()) {
                if (eventHandlers.get(type).remove(handler)) {
                    break;
                }
            }
        }
        registeredHandlers.removeAll(tree);
    }

    void handle(ForestEvent event) {
        for (Consumer<ForestEvent> handler: eventHandlers.get(event.getClass())) {
            handler.accept(event);
        }
    }
}
