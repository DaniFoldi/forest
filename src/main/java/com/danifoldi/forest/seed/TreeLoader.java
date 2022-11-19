package com.danifoldi.forest.seed;

import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlArray;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.util.FileUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeLoader {

    private final List<String> targets = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Set<String>> flattenedTargetDependencies = new ConcurrentHashMap<>();
    private final Map<String, TreeInfo> knownTrees = new ConcurrentHashMap<>();


    public void preloadKnownTrees() {
        for (Path jar: Microbase.getDatafolder().resolve("trees")) {
            try {
                ClassLoader loader = URLClassLoader.newInstance(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
                String file = jar.getFileName().toString();
                String pack = file.toLowerCase(Locale.ROOT);
                String clazz = file.substring(0, 1).toUpperCase(Locale.ROOT) + file.substring(1).toLowerCase(Locale.ROOT);
                if (knownTrees.getOrDefault(pack, TreeInfo.EMPTY).loaded) {
                    Microbase.logger.log(Level.INFO, "Tree %s is currently loaded. Unload to update cache".formatted(pack));
                } else {
                    knownTrees.put(pack, new TreeInfo(loader, "com.danifoldi.forest.tree.%s.%sTree".formatted(pack, clazz), pack));
                }
            } catch (MalformedURLException e) {
                Microbase.logger.log(Level.WARNING, "Could not load %s url".formatted(jar.getFileName().toString()));
            }
        }
    }

    public CompletableFuture<Boolean> fetchTargets() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileUtil.ensureDmlFile(Microbase.getDatafolder(), "targets.dml");
                DmlArray values = DmlParser.parse(Microbase.getDatafolder().resolve("targets.dml")).asArray();
                targets.clear();
                targets.addAll(values.value().stream().map(v -> v.asString().value()).toList());
                return true;
            } catch (IOException | DmlParseException e) {
                Microbase.logger.log(Level.SEVERE, e.getMessage());
                return false;
            }
        }, Microbase.getThreadPool());
    }

    public CompletableFuture<Boolean> loadTree(String name) {
        if (name.contains(">=")) {
            return loadTree(name.split(">=")[0], name.split(">=")[1]);
        } else {
            return loadTree(name, "*");
        }
    }

    public boolean versionMatches(String version, String requirement) {
        if (requirement.equals("*")) {
            return true;
        }

        if (requirement.startsWith(">=")) {
            String minVersion = requirement.substring(2);
            String[] versionComponents = version.split("\\.");
            String[] minComponents = minVersion.split("\\.");
            // TODO allocate arrays in a single go
            while (versionComponents.length < minComponents.length) {
                versionComponents = Stream.concat(Arrays.stream(versionComponents), Stream.of("0")).toArray(String[]::new);
            }
            while (minComponents.length < versionComponents.length) {
                minComponents = Stream.concat(Arrays.stream(minComponents), Stream.of("0")).toArray(String[]::new);
            }

            try {
                for (int i = 0; i < minComponents.length; i++) {
                    if (Integer.parseInt(minComponents[i]) < Integer.parseInt(versionComponents[i])) {
                        return false;
                    }
                }
            } catch (NumberFormatException e) {
                Microbase.logger.log(Level.WARNING, "Could not check version %s against requirement %s".formatted(version, requirement));
                return false;
            }
            return true;
        } else {
            Microbase.logger.log(Level.WARNING, "Could not parse requirement %s".formatted(requirement));
            return false;
        }
    }

    public CompletableFuture<Boolean> loadTree(String name, String versionRequirement) {
        return CompletableFuture.supplyAsync(() -> {
            if (!knownTrees.containsKey(name)) {
                Microbase.logger.log(Level.WARNING, "Cannot find tree %s to load", name);
                return false;
            }

            TreeInfo tree = knownTrees.get(name);
            if (!versionMatches(tree.version, versionRequirement)) {
                Microbase.logger.log(Level.SEVERE, "Tree %s has version %s which is not compatible with %s".formatted(name, tree.version, versionRequirement));
                return false;
            }

            for (String dependency: knownTrees.get(name).dependencies) {
                if (!loadTree(dependency).join()) {
                    return false;
                }
            }

            if (tree.loaded) {
                return true;
            }

            if (!tree.makeTree()) {
                return false;
            }

            tree.tree.load().join();

            return true;
        }, Microbase.getThreadPool());
    }

    public CompletableFuture<Boolean> unloadTree(String name, boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            if (!knownTrees.containsKey(name)) {
                Microbase.logger.log(Level.WARNING, "Cannot find tree %s to unload", name);
                return false;
            }

            TreeInfo tree = knownTrees.get(name);
            if (!tree.loaded) {
                return true;
            }

            if (!tree.tree.unload(force).join()) {
                return false;
            }

            for (String dependency: knownTrees.get(name).dependencies) {
                if (!unloadTree(dependency, force).join()) {
                    return false;
                }
            }
            return true;

        }, Microbase.getThreadPool());
    }

    public CompletableFuture<Boolean> loadTargets() {
        return CompletableFuture.supplyAsync(() -> {
            for (String target : targets) {
                if (!loadTarget(target).join()) {
                    return false;
                }
            }
            return true;
        });
    }

    public CompletableFuture<Boolean> unloadTargets(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            for (String target : targets) {
                if (!unloadTarget(target, force).join()) {
                    return false;
                }
            }
            return true;
        });
    }

    private void addDependenciesOf(String name, String toTarget) {
        if (flattenedTargetDependencies.get(toTarget).add(name)) {
            for (String dependency: knownTrees.get(name).dependencies) {
                addDependenciesOf(dependency, toTarget);
            }
        }
    }

    public CompletableFuture<Boolean> loadTarget(String name) {
        return CompletableFuture.supplyAsync(() -> {
            flattenedTargetDependencies.putIfAbsent(name, Collections.synchronizedSet(new TreeSet<>()));
            addDependenciesOf(name, name);
            return loadTree(name).join();
        });
    }

    public CompletableFuture<Boolean> unloadTarget(String name, boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            if (!unloadTree(name, force).join()) {
                return false;
            }

            flattenedTargetDependencies.get(name).clear();
            Map<String, TreeInfo> unloadQueue = knownTrees
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().loaded)
                    .filter(e -> !targets.contains(e.getKey()))
                    .filter(e -> flattenedTargetDependencies
                            .entrySet()
                            .stream()
                            .noneMatch(d -> d.getValue().contains(e.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            for (Map.Entry<String, TreeInfo> entry: unloadQueue.entrySet()) {
                if (!entry.getValue().tree.unload(force).join()) {
                    return false;
                }
            }
            return true;
        });
    }
}
