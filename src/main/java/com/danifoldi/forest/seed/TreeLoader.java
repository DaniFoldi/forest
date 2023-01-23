package com.danifoldi.forest.seed;

import com.danifoldi.dml.DmlParser;
import com.danifoldi.dml.exception.DmlParseException;
import com.danifoldi.dml.type.DmlArray;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.microbase.Microbase;
import com.danifoldi.microbase.MicrobasePlatformType;
import com.danifoldi.microbase.util.FileUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    static final Map<String, TreeInfo> knownTrees = new ConcurrentHashMap<>();

    private static String serverId = "";

    public static String getServerId() {
        return serverId;
    }

    private static TreeLoader instance;

    public static void setInstance(TreeLoader instance) {
        TreeLoader.instance = instance;
    }

    public static TreeLoader getInstance() {
        return instance;
    }


    public void preloadKnownTrees() {
        try {
            FileUtil.ensureFolder(Microbase.getDatafolder());
            FileUtil.ensureFolder(Microbase.getDatafolder().resolve("trees"));
        } catch (IOException e) {
            Microbase.logger.log(Level.SEVERE, "Could not create folder for trees, something is probably wrong");
            return;
        }

        List<URL> urls = new ArrayList<>();

        try (Stream<Path> files = Files.list(Microbase.getDatafolder().resolve("trees"))) {
            files.forEach(path -> {
                try {
                    urls.add(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    Microbase.logger.log(Level.SEVERE, "Could not load tree jar url %s".formatted(path));
                }
            });
        } catch (IOException e) {
            Microbase.logger.log(Level.SEVERE, "Could not load tree jar list");
            return;
        }

        URLClassLoader loader = URLClassLoader.newInstance(urls.toArray(URL[]::new), getClass().getClassLoader());

        try (Stream<Path> files = Files.list(Microbase.getDatafolder().resolve("trees"))) {
            for (Iterator<Path> it = files.iterator(); it.hasNext(); ) {
                Path jar = it.next();
                String file = jar.getFileName().toString();
                String pack = file.toLowerCase(Locale.ROOT).replaceFirst("^[Ff]orest-?", "").replaceFirst("-\\d.*", "").replaceFirst("\\.jar$", "");
                if (knownTrees.getOrDefault(pack, TreeInfo.EMPTY).loaded) {
                    Microbase.logger.log(Level.INFO, "Tree %s is currently loaded. Unload to update cache".formatted(pack));
                } else {
                    TreeInfo treeInfo = new TreeInfo(loader, "com.danifoldi.forest.tree.%s.%sTree".formatted(pack, pack.substring(0, 1).toUpperCase(Locale.ROOT) + pack.substring(1).toLowerCase(Locale.ROOT)), pack);
                    treeInfo.loadTreeInfo();
                    knownTrees.put(pack, treeInfo);
                    Microbase.logger.log(Level.INFO, "Found tree %s in %s".formatted(pack, file));
                }
            }
        } catch (IOException e) {
            Microbase.logger.log(Level.SEVERE, "Could not load available trees");
        }
    }

    public CompletableFuture<Boolean> fetchMetadata() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileUtil.ensureDmlFile(Microbase.getDatafolder(), "metadata.dml");
                DmlObject metadata = DmlParser.parse(Microbase.getDatafolder().resolve("metadata.dml")).asObject();
                DmlArray values = metadata.getArray("targets");
                serverId = metadata.getString("server").asString().value();
                targets.clear();
                targets.addAll(values.value().stream().map(v -> v.asString().value()).toList());
                return true;
            } catch (IOException | DmlParseException e) {
                Microbase.logger.log(Level.SEVERE, "Could not load forest metadata");
                Microbase.logger.log(Level.SEVERE, e.getMessage());
                return false;
            }
        }, Microbase.getThreadPool("forest"));
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

        if (requirement.matches("\\d+\\.\\d+\\.\\d+")) {
            requirement = ">=" + requirement;
        }

        if (requirement.startsWith(">=")) {
            String minVersion = requirement.substring(2);
            String[] versionComponents = version.split("\\.");
            String[] minComponents = minVersion.split("\\.");
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

    public boolean supportsPlatform(Set<String> supported, MicrobasePlatformType platformType) {
        return supported.isEmpty() || supported.stream().anyMatch(platformType.name()::equalsIgnoreCase);
    }

    public CompletableFuture<Boolean> loadTree(String name, String versionRequirement) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.logger.log(Level.INFO, "Growing tree %s".formatted(name));
            if (!knownTrees.containsKey(name)) {
                Microbase.logger.log(Level.WARNING, "Cannot find tree %s to load".formatted(name));
                return false;
            }

            TreeInfo tree = knownTrees.get(name);
            if (tree.loaded) {
                return true;
            }

            if (!versionMatches(tree.version, versionRequirement)) {
                Microbase.logger.log(Level.SEVERE, "Tree %s has version %s which is not compatible with %s required".formatted(name, tree.version, versionRequirement));
                return false;
            }

            if (!supportsPlatform(tree.platforms, Microbase.platformType)) {
                Microbase.logger.log(Level.SEVERE, "Tree %s only supports platforms %s, but was loaded on %s".formatted(name, String.join(",", tree.platforms), Microbase.platformType.name()));
                return false;
            }

            for (Map.Entry<String, String> dependency: knownTrees.get(name).dependencies.entrySet()) {
                if (!loadTree(dependency.getKey(), dependency.getValue()).join()) {
                    return false;
                }
            }

            Microbase.logger.log(Level.INFO, "Loading tree %s".formatted(name));

            if (!tree.makeTree()) {
                return false;
            }

            tree.tree.load().join();
            tree.loaded = true;

            return true;
        }, Microbase.getThreadPool("forest"));
    }

    public CompletableFuture<Boolean> unloadTree(String name, boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.logger.log(Level.INFO, "Unloading tree %s".formatted(name));
            if (!knownTrees.containsKey(name)) {
                Microbase.logger.log(Level.WARNING, "Cannot find tree %s to unload".formatted(name));
                return false;
            }

            TreeInfo tree = knownTrees.get(name);
            if (!tree.loaded) {
                return true;
            }

            if (!tree.tree.unload(force).join()) {
                return false;
            }
            tree.loaded = false;

            for (String dependency: knownTrees.get(name).dependencies.keySet()) {
                if (!unloadTree(dependency, force).join()) {
                    return false;
                }
            }
            return true;

        }, Microbase.getThreadPool("forest"));
    }

    public CompletableFuture<Boolean> loadTargets() {
        return CompletableFuture.supplyAsync(() -> {
            for (String target: targets) {
                if (!loadTarget(target).join()) {
                    return false;
                }
            }
            return true;
        });
    }

    public CompletableFuture<Boolean> unloadTargets(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            for (String target: targets) {
                if (!unloadTarget(target, force).join()) {
                    return false;
                }
            }
            return true;
        });
    }

    private void addDependenciesOf(String name, String toTarget) {
        if (flattenedTargetDependencies.get(toTarget).add(name)) {
            for (String dependency: knownTrees.get(name).dependencies.keySet()) {
                addDependenciesOf(dependency, toTarget);
            }
        }
    }

    public CompletableFuture<Boolean> loadTarget(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.logger.log(Level.INFO, "Loading target %s".formatted(name));
            if (!knownTrees.containsKey(name)) {
                Microbase.logger.log(Level.SEVERE, "Could not find target %s to load".formatted(name));
                return false;
            }
            flattenedTargetDependencies.putIfAbsent(name, Collections.synchronizedSet(new TreeSet<>()));
            addDependenciesOf(name, name);
            return loadTree(name).join();
        });
    }

    public CompletableFuture<Boolean> unloadTarget(String name, boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.logger.log(Level.INFO, "Unloading target %s".formatted(name));
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
