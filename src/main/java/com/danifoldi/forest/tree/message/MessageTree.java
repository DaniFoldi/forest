package com.danifoldi.forest.tree.message;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedDataVerse;
import com.danifoldi.dataverse.util.Pair;
import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MessageTree implements Tree {

    public NamespacedDataVerse<Message> messages;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            messages = DataVerse.getDataVerse().getNamespacedDataVerse(new DataverseNamespace(), "message", Message::new);
            messages.list().thenAccept(m -> MessageProvider.update(m.stream().collect(Collectors.toMap(Pair::getFirst, p -> p.getSecond().value))));
        }, Microbase.getThreadPool());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.completedFuture(true);
    }
}
