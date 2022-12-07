package com.danifoldi.forest.tree.mail;

public class Mail {
    String sender;
    String message;
    boolean read = false;

    public boolean isRead() {
        return read;
    }

    public Mail() {

    }

    public Mail(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }
}
