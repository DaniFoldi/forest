package com.danifoldi.forest.tree.remote;

import com.danifoldi.forest.tree.task.Task;
import hazelnut.core.Message;
import org.jetbrains.annotations.NotNull;

public class RemoteTask extends Task implements Message<Task> {
    public RemoteTask(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public @NotNull Class<Task> type() {
        return Task.class;
    }
}
