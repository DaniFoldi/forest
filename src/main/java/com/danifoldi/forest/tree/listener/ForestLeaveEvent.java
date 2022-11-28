package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BasePlayer;

public interface ForestLeaveEvent extends ForestEvent {

    Reason reason();

    BasePlayer player();

    enum Reason {
        DISCONNECT,
        KICK,
        TIMEOUT,
        INVALID
    }
}
