package com.danifoldi.forest.seed;

import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.microbase.Microbase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TreeInfo {
    public String version;
    public Set<String> dependencies;
    public ClassLoader classLoader;
    public String className;
    public String pack;
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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(treeClass.getResourceAsStream("trees/%s.dml".formatted(pack))))) {
                String dmlstring = reader.lines().collect(Collectors.joining("\n"));
                DmlObject dml = DmlParser.parse(dmlstring).asObject();
                version = dml.get("version").toString();
                dependencies = dml.get("dependencies").asArray().value().stream().map(v -> v.asString().value()).collect(Collectors.toSet());
            } catch (DmlParseException | IOException e) {
                Microbase.logger.log(Level.SEVERE, "Could not load tree metadata %s".formatted(pack));
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
