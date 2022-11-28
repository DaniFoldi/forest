package com.danifoldi.forest.tree.task;

import hazelnut.core.Message;
import org.jetbrains.annotations.NotNull;

public class Task implements Message<Task> {
    public String type;
    public String value;

    public Task(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Task() {

    }

    @Override
    public @NotNull Class<Task> type() {
        return Task.class;
    }
}