package com.danifoldi.forest.tree.paginate;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@VersionCollector("1.0.0")
public class PaginateTree implements Tree {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.completedFuture(true);
    }

    public List<BaseMessage> generatePagination(String messageKeyPrefix, int page, int pageSize, BiFunction<BaseMessage, Integer, BaseMessage> entry, Supplier<Integer> itemCountSupplier, String suggestionCommand, String target) {
        LinkedList<BaseMessage> result = new LinkedList<>();

        int itemCount = itemCountSupplier != null ? Optional.of(itemCountSupplier.get()).orElse(-1) : -1;
        int pageCount = itemCount < 0 ? -1 : (int)Math.ceil((double)itemCount / (double)pageSize);

        BaseMessage header = Microbase.baseMessage();
        BaseMessage footer = Microbase.baseMessage();

        if (page > 1) {
            footer = footer
                    .providedText("%s.previous".formatted(messageKeyPrefix))
                    .click(BaseMessage.ClickType.RUN_COMMAND)
                    .rawText("%s %d".formatted(suggestionCommand, page - 1))
                    .text();
        }

        header = header
                .providedText("%s.header".formatted(messageKeyPrefix))
                .center()
                .replace("{page}", String.valueOf(page))
                .replace("{target}", target);

        footer = footer
                .providedText("%s.footer".formatted(messageKeyPrefix))
                .center()
                .replace("{page}", String.valueOf(page));

        if (itemCount >= 0) {
            header = header
                    .replace("{items}", String.valueOf(itemCount));
            footer = footer
                    .replace("{items}", String.valueOf(itemCount));
        }

        if (pageCount >= 0) {
            header = header
                    .replace("{pages}", String.valueOf(pageCount));
            footer = footer
                    .replace("{pages}", String.valueOf(pageCount));
        }

        if (pageCount < 0 || page < pageCount) {
            footer = footer
                    .text()
                    .providedText("%s.next".formatted(messageKeyPrefix))
                    .click(BaseMessage.ClickType.RUN_COMMAND)
                    .rawText("%s %d".formatted(suggestionCommand, page + 1));
        }

        for (int i = 1; i <= pageSize; i++) {
            int id = ((page - 1) * pageSize + i);
            result.add(entry.apply(Microbase.baseMessage().providedText("%s.entry".formatted(messageKeyPrefix)), id)
                    .replace("{id}", String.valueOf(id))
                    .replace("{target}", target));
        }

        result.addFirst(header);
        result.addLast(footer);

        return result;
    }
}
