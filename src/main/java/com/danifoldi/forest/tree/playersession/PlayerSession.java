package com.danifoldi.forest.tree.playersession;

import java.time.Instant;
import java.util.UUID;

public class PlayerSession {
    public String playerName;
    public UUID uuid;
    public Instant joined;
    public Instant left;
    public String ip;
    public String joinAddress;
    public int protocolVersion;
    public String clientBrand;

    public PlayerSession() {

    }

    public PlayerSession(String playerName, UUID uuid, Instant joined, String ip, String joinAddress, int protocolVersion) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.joined = joined;
        this.ip = ip;
        this.joinAddress = joinAddress;
        this.protocolVersion = protocolVersion;
    }
}
