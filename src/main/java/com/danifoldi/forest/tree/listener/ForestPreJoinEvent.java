package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BaseMessage;

import java.net.InetAddress;
import java.util.UUID;

public interface ForestPreJoinEvent extends ForestCancelableEvent {

    InetAddress ipAddress();
    String playerName();
    String hostname();
    UUID uuid();

    void kickMessage(BaseMessage message);
}
