package com.danifoldi.forest.seed.collector.collector;

import com.danifoldi.dml.DmlSerializer;
import com.danifoldi.dml.type.DmlArray;
import com.danifoldi.dml.type.DmlObject;
import com.danifoldi.dml.type.DmlString;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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
                DmlObject out = new DmlObject(new HashMap<>());
                out.set("commands", commands);
                out.set("messages", messages);
                out.set("permissions", permissions);
                collected.put(treeName, out);
            }
            return collected.get(treeName);
        };

        try {
            for (TypeElement annotation : annotations) {
                switch (annotation.getSimpleName().toString()) {
                    case "CommandCollector":
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (CommandCollector c: element.getAnnotationsByType(CommandCollector.class)) {
                                collectForTree.apply(element).get("commands").asArray().add(new DmlString(c.value()));
                            }
                        }
                        break;
                    case "MessageCollector":
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (MessageCollector m: element.getAnnotationsByType(MessageCollector.class)) {
                                DmlObject message = new DmlObject(new HashMap<>());
                                message.set("template", new DmlString(m.value()));
                                DmlArray replacements = new DmlArray(new ArrayList<>());
                                message.set("replacements", replacements);
                                collectForTree.apply(element).get("messages").asArray().add(message);
                            }
                        }
                        break;
                    case "PermissionCollector":
                        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                            for (PermissionCollector p: element.getAnnotationsByType(PermissionCollector.class)) {
                                collectForTree.apply(element).get("permissions").asArray().add(new DmlString(p.value()));
                            }
                        }
                        break;
                }
            }
            for (Map.Entry<String, DmlObject> tree: collected.entrySet()) {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "trees/%s-collected.dml".formatted(tree.getKey()));
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
