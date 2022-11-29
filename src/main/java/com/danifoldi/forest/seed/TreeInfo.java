package com.danifoldi.forest.seed;

import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.dml.type.DmlString;
import com.danifoldi.dml.type.DmlValue;
import com.danifoldi.microbase.Microbase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TreeInfo {
    public String version;
    public Map<String, String> dependencies;
    public Set<String> platforms;
    public Set<String> commands;
    public Map<String, List<String>> messages;
    public final ClassLoader classLoader;
    public final String className;
    public final String pack;
    public Class<? extends Tree> treeClass;
    public Tree tree;
    public boolean loaded = false;

    static TreeInfo EMPTY = new TreeInfo(new URLClassLoader(new URL[0]), "empty", "empty");

    public TreeInfo(ClassLoader classLoader, String className, String pack) {
         this.classLoader = classLoader;
         this.className = className;
         this.pack = pack;
    }

    public boolean makeTree() {
        try {
            treeClass = Class.forName(className, true, classLoader).asSubclass(Tree.class);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(treeClass.getResourceAsStream("trees/%s.dml".formatted(pack)))))) {
                String dmlString = reader.lines().collect(Collectors.joining("\n"));
                DmlObject dml = DmlParser.parse(dmlString).asObject();
                version = dml.get("version").toString();
                commands = dml.get("commands").asArray().value().stream().map(DmlValue::asString).map(DmlString::value).collect(Collectors.toSet());
                platforms = dml.get("platforms").asArray().value().stream().map(DmlValue::asString).map(DmlString::value).collect(Collectors.toSet());
                messages = dml.get("messages").asArray().value().stream().map(DmlValue::asObject).collect(Collectors.toMap(v -> v.get("template").asString().value(), v -> v.get("replacements").asArray().value().stream().map(r -> r.asString().value()).toList()));
                dependencies = dml.get("dependencies").asArray().value().stream().map(DmlValue::asObject).collect(Collectors.toMap(v -> v.get("tree").asString().value(), v -> v.get("minVersion").asString().value()));
            } catch (DmlParseException | IOException e) {
                Microbase.logger.log(Level.SEVERE, "Could not load tree metadata %s: %s".formatted(pack, e.getMessage()));
                return false;
            }
            tree = treeClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            Microbase.logger.log(Level.WARNING, "Could not load tree class %s".formatted(className));
            return false;
        }
        return true;
    }
}
