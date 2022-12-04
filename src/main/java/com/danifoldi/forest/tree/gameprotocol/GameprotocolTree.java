package com.danifoldi.forest.tree.gameprotocol;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedDataVerse;
import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlDocument;
import com.danifoldi.dml.type.DmlKey;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

@VersionCollector("1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
public class GameprotocolTree implements Tree {

    private final String protocols = "https://raw.githubusercontent.com/DaniFoldi/forest/main/src/main/resources/protocols.dml";
    private NamespacedDataVerse<ProtocolInfo> protocolDataverse;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            protocolDataverse = DataVerse.getDataVerse().getNamespacedDataVerse(DataverseNamespace.get(), "gameprotocol", ProtocolInfo::new);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(GameprotocolTree.class.getResourceAsStream("/protocols.dml"))))) {
                DmlObject protocols = DmlParser.parse(reader.lines().collect(Collectors.joining("\n"))).asObject();
                for (DmlKey key: protocols.keys()) {
                    DmlObject protocol = protocols.get(key).asObject();
                    protocolDataverse.createOrUpdate(key.value(), new ProtocolInfo(protocol.get("min").asString().value(), protocol.get("max").asString().value(), protocol.get("nice").asString().value()));
                }
            } catch (IOException e) {
                Microbase.logger.log(Level.WARNING, "Failed to fetch known protocols file");
            } catch (DmlParseException e) {
                Microbase.logger.log(Level.WARNING, "Failed to parse known protocols file");
            }
            try {
                String newProtocols = HttpClient.newHttpClient()
                        .sendAsync(HttpRequest
                                .newBuilder()
                                .uri(URI.create(protocols))
                                .build(), HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .join();
                DmlObject parsedProtocols = DmlParser.parse(newProtocols).asObject();
                for (DmlKey key: parsedProtocols.keys()) {
                    DmlObject protocol = parsedProtocols.get(key).asObject();
                    protocolDataverse.create(key.value(), new ProtocolInfo(protocol.get("min").asString().value(), protocol.get("max").asString().value(), protocol.get("nice").asString().value()));
                }
            } catch (DmlParseException e) {
                Microbase.logger.log(Level.WARNING, "Failed to fetch new protocol information");
            }
        }, Microbase.getThreadPool("gameprotocol"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("gameprotocol", 1000, force));
    }

    public CompletableFuture<String> minVersion(int protocol) {
        return CompletableFuture.supplyAsync(() -> {
            ProtocolInfo info = protocolDataverse.get(String.valueOf(protocol)).join();
            return info == null ? "Unknown" : info.minVersion;
        }, Microbase.getThreadPool("gameprotocol"));
    }

    public CompletableFuture<String> maxVersion(int protocol) {
        return CompletableFuture.supplyAsync(() -> {
            ProtocolInfo info = protocolDataverse.get(String.valueOf(protocol)).join();
            return info == null ? "Unknown" : info.maxVersion;
        }, Microbase.getThreadPool("gameprotocol"));
    }

    public CompletableFuture<String> niceVersion(int protocol) {
        return CompletableFuture.supplyAsync(() -> {
            ProtocolInfo info = protocolDataverse.get(String.valueOf(protocol)).join();
            return info == null ? "Unknown" : info.niceVersion;
        }, Microbase.getThreadPool("gameprotocol"));
    }
}
