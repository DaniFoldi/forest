package com.danifoldi.forest.tree.listener;

public interface ForestCancelableEvent extends ForestEvent {
    boolean isCancelled();
    void cancel(boolean cancelled);
}
