package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BasePlayer;

public interface ForestCommandEvent extends ForestCancelableEvent {

    BasePlayer player();

    String command();
}
