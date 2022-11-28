package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.BasePlayer;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PaperListener implements Listener {

    private final ListenerTree tree;

    public PaperListener(ListenerTree tree) {
        this.tree = tree;
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        tree.handle(new ForestPreJoinEvent() {
            @Override
            public boolean isCancelled() {
                return event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED;
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setLoginResult(cancelled ? AsyncPlayerPreLoginEvent.Result.KICK_OTHER : AsyncPlayerPreLoginEvent.Result.ALLOWED);
            }

            @Override
            public InetAddress ipAddress() {
                return event.getAddress();
            }

            @Override
            public String playerName() {
                return event.getName();
            }

            @Override
            public String hostname() {
                return event.getHostname();
            }

            @Override
            public UUID uuid() {
                return event.getUniqueId();
            }

            @Override
            public void kickMessage(BaseMessage message) {
                event.kickMessage(message.convert());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerLoginEvent event) {
        tree.handle(new ForestJoinEvent() {
            @Override
            public boolean isCancelled() {
                return event.getResult() != PlayerLoginEvent.Result.ALLOWED;
            }

            @Override
            public void cancel(boolean cancelled) {
                event.setResult(cancelled ? PlayerLoginEvent.Result.KICK_OTHER : PlayerLoginEvent.Result.ALLOWED);
            }

            @Override
            public InetAddress ipAddress() {
                return event.getAddress();
            }

            @Override
            public String playerName() {
                return event.getPlayer().getName();
            }

            @Override
            public String hostname() {
                return event.getHostname();
            }

            @Override
            public UUID uuid() {
                return event.getPlayer().getUniqueId();
            }

            @Override
            public void kickMessage(BaseMessage message) {
                event.kickMessage(message.convert());
            }
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
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
                return Microbase.toBasePlayer(event.getPlayer());
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
    public void onServerCommand(ServerCommandEvent event) {
        tree.handle(new ForestServerCommandEvent() {
            @Override
            public String command() {
                return event.getCommand();
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
    public void onSendCommand(PlayerCommandSendEvent event) {
        tree.handle(new ForestSendCommandsEvent() {
            @Override
            public BasePlayer player() {
                return Microbase.toBasePlayer(event.getPlayer());
            }

            @Override
            public Collection<String> commands() {
                return event.getCommands();
            }
        });
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        tree.handle(new ForestCommandEvent() {
            @Override
            public BasePlayer player() {
                return Microbase.toBasePlayer(event.getPlayer());
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
    public void onLeave(PlayerQuitEvent event) {
        tree.handle(new ForestLeaveEvent() {
            @Override
            public Reason reason() {
                return switch (event.getReason()) {
                    case KICKED -> Reason.KICK;
                    case DISCONNECTED -> Reason.DISCONNECT;
                    case ERRONEOUS_STATE -> Reason.INVALID;
                    case TIMED_OUT -> Reason.TIMEOUT;
                };
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
                return event.getCompletions();
            }

            @Override
            public void setCompletions(List<String> completions) {
                event.setCompletions(completions);
            }

            @Override
            public BaseSender sender() {
                return Microbase.toBaseSender(event.getSender());
            }

            @Override
            public String prompt() {
                return event.getBuffer();
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
