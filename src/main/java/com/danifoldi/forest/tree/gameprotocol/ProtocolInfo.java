package com.danifoldi.forest.tree.gameprotocol;

public class ProtocolInfo {
    String minVersion;
    String maxVersion;
    String niceVersion;

    public ProtocolInfo() {

    }

    public ProtocolInfo(String minVersion, String maxVersion, String niceVersion) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.niceVersion = niceVersion;
    }
}
