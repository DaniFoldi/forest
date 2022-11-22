package com.danifoldi.forest.tree.dataverse;

import com.danifoldi.dataverse.data.Namespaced;
import org.jetbrains.annotations.NotNull;

public class DataverseNamespace implements Namespaced {

    public static DataverseNamespace get() {
        return instance;
    }

    private static DataverseNamespace instance = new DataverseNamespace();

    @Override
    public @NotNull String getNamespace() {
        return "Forest";
    }
}
