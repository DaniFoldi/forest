package com.danifoldi.forest.tree.manager;

import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.DmlSerializer;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlArray;
import com.danifoldi.dml.type.DmlBoolean;
import com.danifoldi.dml.type.DmlKey;
import com.danifoldi.dml.type.DmlNumber;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.dml.type.DmlString;
import com.danifoldi.dml.type.DmlValue;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.TreeInfo;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.tree.logger.LogEntry;
import com.danifoldi.forest.tree.logger.LoggerTree;
import com.danifoldi.forest.tree.message.Message;
import com.danifoldi.forest.tree.message.MessageTree;
import com.danifoldi.microbase.Microbase;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagerListener extends WebSocketListener {

    private Consumer<LogEntry> logStream;

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        webSocket.send("{connected: true}");
        logStream = entry -> {
            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                    new DmlKey("type"), new DmlString("log"),
                    new DmlKey("level"), new DmlNumber(BigDecimal.valueOf(entry.level)),
                    new DmlKey("sequenceNumber"), new DmlNumber(BigDecimal.valueOf(entry.sequenceNumber)),
                    new DmlKey("sourceClass"), new DmlString(entry.sourceClass),
                    new DmlKey("sourceMethod"), new DmlString(entry.sourceMethod),
                    new DmlKey("message"), new DmlString(entry.message),
                    new DmlKey("logger"), new DmlString(entry.logger),
                    new DmlKey("resourceBundle"), new DmlString(entry.resourceBundle),
                    new DmlKey("time"), new DmlString(entry.time.toString()),
                    new DmlKey("stacktrace"), new DmlString("%s____%s".formatted(entry.throwable.getMessage(), Arrays.stream(entry.throwable.getStackTrace()).map(StackTraceElement::toString)))
            ))));
        };
        GrownTrees.get(LoggerTree.class).addHandler(logStream);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        try {
            DmlObject message = DmlParser.parse(text).asObject();
            String id = message.getString("id").value();
            try {
                switch (message.getString("type").value()) {
                    case "apiVersion" -> webSocket.send(DmlSerializer.serialize(new DmlObject(
                                Map.of(new DmlKey("id"), new DmlString(id),
                                        new DmlKey("apiVersion"), new DmlNumber(BigDecimal.valueOf(1))))));
                    case "trees" -> webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                            new DmlKey("id"), new DmlString(id),
                            new DmlKey("trees"), new DmlObject(
                                    GrownTrees
                                            .getKnownTrees()
                                            .entrySet()
                                            .stream()
                                            .collect(Collectors.toMap(e -> new DmlKey(e.getKey()), e -> {
                                                TreeInfo info = e.getValue();
                                                DmlObject result = new DmlObject(new HashMap<>());
                                                result.set("loaded", new DmlBoolean(info.loaded));
                                                result.set("version", new DmlString(info.version));
                                                result.set("commands", new DmlArray(info.commands.stream().map(c -> (DmlValue) new DmlString(c)).toList()));
                                                result.set("platforms", new DmlArray(info.platforms.stream().map(p -> (DmlValue) new DmlString(p)).toList()));
                                                result.set("messages", new DmlObject(info.messages.entrySet().stream().collect(Collectors.toMap(m -> new DmlKey(m.getKey()), m -> new DmlArray(m.getValue().stream().map(v -> (DmlValue) new DmlString(v)).toList())))));
                                                result.set("dependencies", new DmlObject(info.dependencies.entrySet().stream().collect(Collectors.toMap(m -> new DmlKey(m.getKey()), m -> new DmlString(m.getValue())))));
                                                return result;
                                            }))
                            )
                    ))));
                    case "loadTree" -> TreeLoader.getInstance().loadTree(message.getString("tree").value()).thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "unloadTree" -> TreeLoader.getInstance().unloadTree(message.getString("tree").value(), message.getBoolean("force").value()).thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "loadTarget" -> TreeLoader.getInstance().loadTarget(message.getString("tree").value()).thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "unloadTarget" -> TreeLoader.getInstance().unloadTarget(message.getString("tree").value(), message.getBoolean("force").value()).thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "loadTargets" -> TreeLoader.getInstance().loadTargets().thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "unloadTargets" -> TreeLoader.getInstance().unloadTargets(message.getBoolean("force").value()).thenAcceptAsync(success -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(success)))));
                    }, Microbase.getThreadPool("manager"));
                    case "messageList" -> GrownTrees.get(MessageTree.class).messages.list().thenAcceptAsync(messages -> {
                           webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                   new DmlKey("id"), new DmlString(id),
                                   new DmlKey("messages"), new DmlObject(messages.stream().collect(Collectors.toMap(p -> new DmlKey(p.getFirst()), p -> new DmlString(p.getSecond().value))))
                           )))) ;
                        }, Microbase.getThreadPool("manager"));
                    case "messageUpdate" -> GrownTrees.get(MessageTree.class).messages.createOrUpdate(message.getString("key").value(), new Message(message.getString("value").value())).thenRunAsync(() -> {
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(true)
                        ))));
                    }, Microbase.getThreadPool("manager"));
                    case "metadataGet" -> {
                        try (BufferedReader reader = Files.newBufferedReader(Microbase.getDatafolder().resolve("metadata.dml"))) {
                            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                    new DmlKey("id"), new DmlString(id),
                                    new DmlKey("metadata"), new DmlString(reader.lines().collect(Collectors.joining("\n")))
                            ))));
                        }
                    }
                    case "metadataSet" -> {
                        try (BufferedWriter writer = Files.newBufferedWriter(Microbase.getDatafolder().resolve("metadata.dml"))) {
                            writer.write(message.getString("metadata").value());
                            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                    new DmlKey("id"), new DmlString(id),
                                    new DmlKey("success"), new DmlBoolean(true)
                            ))));
                        }
                    }
                    case "fileList" -> {
                        try (Stream<Path> files = Files.list(Microbase.getDatafolder().resolve(message.getString("path").value()))) {
                            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                    new DmlKey("id"), new DmlString(id),
                                    new DmlKey("files"), new DmlArray(files.map(p -> {
                                        try {
                                            return (DmlValue)new DmlObject(Map.of(
                                                    new DmlKey("name"), new DmlString(p.getFileName().toString()),
                                                    new DmlKey("size"), new DmlNumber(BigDecimal.valueOf(Files.size(p))),
                                                    new DmlKey("edited"), new DmlString(Files.getLastModifiedTime(p).toString()),
                                                    new DmlKey("type"), new DmlString(Files.probeContentType(p))
                                            ));
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).toList())
                            ))));
                        }
                    }
                    case "fileRead" -> {
                        try (BufferedReader reader = Files.newBufferedReader(Microbase.getDatafolder().resolve(message.getString("path").value()))) {
                            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                    new DmlKey("id"), new DmlString(id),
                                    new DmlKey("value"), new DmlString(reader.lines().collect(Collectors.joining("\n")))
                            ))));
                        }
                    }
                    case "fileWrite" -> {
                        try (BufferedWriter writer = Files.newBufferedWriter(Microbase.getDatafolder().resolve(message.getString("path").value()))) {
                            writer.write(message.getString("value").value());
                            webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                    new DmlKey("id"), new DmlString(id),
                                    new DmlKey("success"), new DmlBoolean(true)
                            ))));
                        }
                    }
                    case "fileMkdir" -> {
                        Files.createDirectories(Microbase.getDatafolder().resolve(message.getString("path").value()));
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(true)
                        ))));
                    }
                    case "fileDelete" -> {
                        Files.delete(Microbase.getDatafolder().resolve(message.getString("path").value()));
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(true)
                        ))));
                    }
                    case "fileMove" -> {
                        Files.move(Microbase.getDatafolder().resolve(message.getString("from").value()), Microbase.getDatafolder().resolve(message.getString("to").value()));
                        webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                                new DmlKey("id"), new DmlString(id),
                                new DmlKey("success"), new DmlBoolean(true)
                        ))));
                    }
                }
            } catch (Exception e) {
                webSocket.send(DmlSerializer.serialize(new DmlObject(Map.of(
                        new DmlKey("id"), new DmlString(id),
                        new DmlKey("success"), new DmlBoolean(false),
                        new DmlKey("error"), new DmlString(e.getMessage())))));
                e.printStackTrace();
            }
        } catch (DmlParseException e) {
            Microbase.logger.log(Level.WARNING, e.getMessage());
            webSocket.close(1002, null);
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        webSocket.close(1003, null);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        GrownTrees.get(LoggerTree.class).removeHandler(logStream);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {

    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {

    }
}
