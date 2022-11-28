package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class WaterfallListener implements Listener {

    private final ListenerTree tree;

    public WaterfallListener(ListenerTree tree) {
        this.tree = tree;
    }

    @EventHandler
    public void onPreJoin(PreLoginEvent event) {
        tree.handle(new ForestPreJoinEvent() {
            @Override
            public InetAddress ipAddress() {
                return event.getConnection().getAddress().getAddress();
            }

            @Override
            public String playerName() {
                return event.getConnection().getName();
            }

            @Override
            public String hostname() {
                return event.getConnection().getAddress().getHostString();
            }

            @Override
            public UUID uuid() {
                return event.getConnection().getUniqueId();
            }

            @Override
            public void kickMessage(BaseMessage message) {
                event.setCancelReason(BungeeComponentSerializer.get().serialize(message.convert()));
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setCancelled(cancelled);
            }
        });
    }

    @EventHandler
    public void onJoin(LoginEvent event) {
        tree.handle(new ForestJoinEvent() {
            @Override
            public InetAddress ipAddress() {
                return event.getConnection().getAddress().getAddress();
            }

            @Override
            public String playerName() {
                return event.getConnection().getName();
            }

            @Override
            public String hostname() {
                return event.getConnection().getAddress().getHostString();
            }

            @Override
            public UUID uuid() {
                return event.getConnection().getUniqueId();
            }

            @Override
            public void kickMessage(BaseMessage message) {
                event.setCancelReason(BungeeComponentSerializer.get().serialize(message.convert()));
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setCancelled(cancelled);
            }
        });
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!event.isCommand() || !(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        tree.handle(new ForestChatEvent() {
            @Override
            public String getMessage() {
                return event.getMessage();
            }

            @Override
            public void setMessage(String message) {
                event.setMessage(message);
            }

            @Override
            public BasePlayer player() {
                return Microbase.toBasePlayer(player);
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setCancelled(cancelled);
            }
        });
    }

    @EventHandler
    public void onCommand(ChatEvent event) {
        if (!event.isProxyCommand() || !(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        tree.handle(new ForestCommandEvent() {
            @Override
            public BasePlayer player() {
                return Microbase.toBasePlayer(player);
            }

            @Override
            public String command() {
                return event.getMessage();
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setCancelled(cancelled);
            }
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        tree.handle(new ForestLeaveEvent() {
            @Override
            public Reason reason() {
                // TODO ?
                return null;
            }

            @Override
            public BasePlayer player() {
                return Microbase.toBasePlayer(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        tree.handle(new ForestTabCompleteEvent() {
            @Override
            public List<String> completions() {
                return event.getSuggestions();
            }

            @Override
            public void setCompletions(List<String> completions) {
                event.getSuggestions().clear();
                event.getSuggestions().addAll(completions);
            }

            @Override
            public BaseSender sender() {
                if (event.getSender() instanceof ProxiedPlayer player) {
                    return Microbase.toBasePlayer(player);
                } else {
                    // TODO add to Microbase
                    return Microbase.toBaseSender(ProxyServer.getInstance().getConsole());
                }
            }

            @Override
            public String prompt() {
                return event.getCursor();
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setCancelled(cancelled);
            }
        });
    }
}
