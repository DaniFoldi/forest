package com.danifoldi.forest.seed.collector.collector;

import com.danifoldi.dml.DmlSerializer;
import com.danifoldi.dml.type.DmlArray;
import com.danifoldi.dml.type.DmlBoolean;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.dml.type.DmlString;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SupportedAnnotationTypes("com.danifoldi.forest.seed.collector.*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CollectorProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, DmlObject> collected = new HashMap<>();

        Function<Element, DmlObject> collectForTree = element -> {
            Element el = element;
            while (!el.getKind().isClass() && !el.getKind().isInterface()) {
                el = el.getEnclosingElement();
            }
            String treeName = el.asType().toString().split("\\.")[4];
            if (!collected.containsKey(treeName)) {
                DmlArray commands = new DmlArray(new ArrayList<>());
                DmlArray messages = new DmlArray(new ArrayList<>());
                DmlArray permissions = new DmlArray(new ArrayList<>());
                DmlArray configs = new DmlArray(new ArrayList<>());
                DmlArray platforms = new DmlArray(new ArrayList<>());
                DmlArray dataverses = new DmlArray(new ArrayList<>());
                DmlArray dependencies = new DmlArray(new ArrayList<>());
                DmlObject out = new DmlObject(new HashMap<>());
                out.set("commands", commands);
                out.set("messages", messages);
                out.set("permissions", permissions);
                out.set("configs", configs);
                out.set("platforms", platforms);
                out.set("dataverses", dataverses);
                out.set("dependencies", dependencies);
                out.set("version", new DmlString("0.0.0"));
                collected.put(treeName, out);
            }
            return collected.get(treeName);
        };

        try {
            for (TypeElement annotation : annotations) {
                switch (annotation.getSimpleName().toString().replaceFirst("^Multi", "")) {
                    case "CommandCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (CommandCollector c : element.getAnnotationsByType(CommandCollector.class)) {
                                collectForTree.apply(element).get("commands").asArray().add(new DmlString(c.value()));
                            }
                        }
                    }
                    case "ConfigCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (ConfigCollector c : element.getAnnotationsByType(ConfigCollector.class)) {
                                DmlObject config = new DmlObject(new HashMap<>());
                                config.set("class", new DmlString(roundEnv.getRootElements().stream().filter(e -> e.getKind().isClass()).findFirst().get().getSimpleName().toString()));
                                config.set("set", new DmlString(c.set()));
                                config.set("global", new DmlBoolean(c.global()));
                                collectForTree.apply(element).get("dataverses").asArray().add(config);
                            }
                        }
                    }
                    case "DataverseCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (DataverseCollector d : element.getAnnotationsByType(DataverseCollector.class)) {
                                DmlObject dataverse = new DmlObject(new HashMap<>());
                                dataverse.set("class", new DmlString(roundEnv.getRootElements().stream().filter(e -> e.getKind().isClass()).findFirst().get().getSimpleName().toString()));
                                dataverse.set("dataverse", new DmlString(d.dataverse()));
                                dataverse.set("multi", new DmlBoolean(d.multi()));
                                collectForTree.apply(element).get("dataverses").asArray().add(dataverse);
                            }
                        }
                    }
                    case "DependencyCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (DependencyCollector d : element.getAnnotationsByType(DependencyCollector.class)) {
                                DmlObject dependency = new DmlObject(new HashMap<>());
                                dependency.set("tree", new DmlString(d.tree()));
                                dependency.set("minVersion", new DmlString(d.minVersion()));
                                collectForTree.apply(element).get("dependencies").asArray().add(dependency);
                            }
                        }
                    }
                    case "MessageCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (MessageCollector m : element.getAnnotationsByType(MessageCollector.class)) {
                                DmlObject message = new DmlObject(new HashMap<>());
                                message.set("template", new DmlString(m.value()));
                                DmlArray replacements = new DmlArray(new ArrayList<>());
                                Arrays.stream(m.replacements()).forEach(r -> replacements.add(new DmlString(r)));
                                message.set("replacements", replacements);
                                collectForTree.apply(element).get("messages").asArray().add(message);
                            }
                        }
                    }
                    case "PermissionCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (PermissionCollector p : element.getAnnotationsByType(PermissionCollector.class)) {
                                collectForTree.apply(element).get("permissions").asArray().add(new DmlString(p.value()));
                            }
                        }
                    }
                    case "PlatformCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (PlatformCollector p : element.getAnnotationsByType(PlatformCollector.class)) {
                                collectForTree.apply(element).get("platforms").asArray().add(new DmlString(p.value()));
                            }
                        }
                    }
                    case "VersionCollector" -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (VersionCollector v : element.getAnnotationsByType(VersionCollector.class)) {
                                collectForTree.apply(element).get("version").asString().value(v.value());
                            }
                        }
                    }
                }
            }
            for (Map.Entry<String, DmlObject> tree: collected.entrySet()) {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "trees/%s.dml".formatted(tree.getKey()));
                try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                    writer.write(DmlSerializer.serialize(tree.getValue()));
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
