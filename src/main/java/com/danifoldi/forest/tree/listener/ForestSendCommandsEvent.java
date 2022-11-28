package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BasePlayer;

import java.util.Collection;

public interface ForestSendCommandsEvent extends ForestEvent {

    BasePlayer player();

    Collection<String> commands();
}
