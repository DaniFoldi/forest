package com.danifoldi.forest.tree.listener;

import com.danifoldi.microbase.BaseSender;

import java.util.List;

public interface ForestTabCompleteEvent extends ForestCancelableEvent {

    List<String> completions();
    void setCompletions(List<String> completions);

    BaseSender sender();

    String prompt();
}
