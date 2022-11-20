package com.danifoldi.forest.tree.dataverse;

import com.danifoldi.dataverse.data.Namespaced;
import org.jetbrains.annotations.NotNull;

public class DataverseNamespace implements Namespaced {
    @Override
    public @NotNull String getNamespace() {
        return "Forest";
    }
}
