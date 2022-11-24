package com.danifoldi.forest.seed;

import com.danifoldi.microbase.Microbase;

import java.util.logging.Level;

public class GrownTrees {

    public static<T> T get(Class<T> clazz) {
        String className = clazz.getName();
        String packageName = className.substring(0, className.lastIndexOf('.'));
        String lastPackage = packageName.substring(packageName.indexOf('.'));
        TreeInfo info = TreeLoader.knownTrees.get(lastPackage);
        if (info == null || !info.loaded) {
            Microbase.logger.log(Level.SEVERE, "Attempted to get tree %s with class %s".formatted(lastPackage, className));
            return null;
        }
        //noinspection unchecked
        return (T)info.tree;
    }
}
