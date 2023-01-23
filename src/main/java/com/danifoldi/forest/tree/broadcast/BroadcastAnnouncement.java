package com.danifoldi.forest.tree.broadcast;

import com.danifoldi.microbase.BaseMessage;

public class BroadcastAnnouncement {
    int priority = 0;
    int remainingRepeatCount = 0;
    BaseMessage message;
    String mode = "chat";
    String worldCondition = "";
    String playerCondition = "";
    String permissionCondition = "";
    String serverCondition = "";
}
