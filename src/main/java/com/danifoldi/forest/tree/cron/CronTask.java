package com.danifoldi.forest.tree.cron;

import com.danifoldi.forest.tree.task.Task;

import java.time.Instant;

public class CronTask extends Task {
    public Instant lastRun = Instant.now();
}
