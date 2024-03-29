package com.danifoldi.forest.tree.message;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseTree;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

@VersionCollector("1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
public class MessageTree implements Tree {

    public NamespacedDataVerse<Message> messages;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            messages = DataVerse.getDataVerse().getNamespacedDataVerse(GrownTrees.get(DataverseTree.class), "message", Message::new);
            Microbase.logger.log(Level.INFO, "Loading messages");
            messages.list().thenAcceptAsync(m -> {
                MessageProvider.update(m.stream().collect(Collectors.toMap(Pair::getFirst, p -> p.getSecond().value)));
                Microbase.logger.log(Level.INFO, "Loaded %d messages".formatted(m.size()));
            }, Microbase.getThreadPool("message"));
        }, Microbase.getThreadPool("message"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("message", 1000, force));
    }
}
