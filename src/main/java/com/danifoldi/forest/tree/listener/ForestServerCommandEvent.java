package com.danifoldi.forest.tree.listener;

public interface ForestServerCommandEvent extends ForestCancelableEvent {

    String command();
}
