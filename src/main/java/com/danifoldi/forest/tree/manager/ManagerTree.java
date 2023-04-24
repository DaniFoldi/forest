package com.danifoldi.forest.tree.manager;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.command.CommandTree;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import grapefruit.command.CommandContainer;
import grapefruit.command.CommandDefinition;
import grapefruit.command.parameter.modifier.Source;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@VersionCollector("1.0.0")
@DependencyCollector(tree="command", minVersion="1.0.0")
@DependencyCollector(tree="config", minVersion="1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
@DependencyCollector(tree="logger", minVersion="1.0.0")
@DependencyCollector(tree="message", minVersion="1.0.0")
public class ManagerTree implements Tree, CommandContainer {

    OkHttpClient client = new OkHttpClient();
    List<WebSocket> websockets = Collections.synchronizedList(new ArrayList<>());

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            GrownTrees.get(CommandTree.class).registerCommands(this);
        }, Microbase.getThreadPool("manager"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(CommandTree.class).unregisterCommands(this);
            websockets.forEach(ws -> ws.close(1001, "manager tree shutdown"));
            return Microbase.shutdownThreadPool("manager", 1000, force);
        });
    }

    @CommandDefinition(route="forestpanel", permission="forest.manager.command.connect", runAsync=true)
    @CommandCollector("forestpanel")
    @MessageCollector("manager.connect.connecting")
    @MessageCollector(value="manager.connect.connected", replacements={"{url}"})
    @PermissionCollector("forest.manager.command.connect")
    public void onConnect(@Source BaseSender sender, String secret) {
        UUID uuid = UUID.randomUUID();
        sender.send(Microbase.baseMessage().providedText("manager.connect.connecting"));
        WebSocket webSocket = client.newWebSocket(new Request
                .Builder()
                .url("https://forest.danifoldi.com/api/%s".formatted(uuid))
                .header("User-Agent", "Forest Manager Tree")
                .header("X-Forest-Secret", secret.replaceAll("[^a-zA-Z0-9]", ""))
                .build(),
                new ManagerListener());
        websockets.add(webSocket);
        sender.send(Microbase.baseMessage().providedText("manager.connect.connected").replace("{url}", "https://forest.danifoldi.com/connector/%s".formatted(uuid)));
        Microbase.logger.log(Level.INFO, "%s opened a new panel session with id %s".formatted(sender.name(), uuid.toString()));
    }
}
