package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BasePlayer;

public interface ForestChatEvent extends ForestCancelableEvent {

    String getMessage();
    void setMessage(String message);

    BasePlayer player();
}
