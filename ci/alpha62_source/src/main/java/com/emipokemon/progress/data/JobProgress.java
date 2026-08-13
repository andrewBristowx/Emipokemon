package com.emipokemon.progress.data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class JobProgress {
    /** Legacy alpha.8-alpha.10 field, retained only for automatic migration. */
    public String activeJob = "";
    public Set<String> activeJobs = new LinkedHashSet<>();
    public Map<String, Long> experience = new HashMap<>();
    public Map<String, Long> completedActions = new HashMap<>();

    public void normalize() {
        if (activeJob == null) activeJob = "";
        if (activeJobs == null) activeJobs = new LinkedHashSet<>();
        else activeJobs = new LinkedHashSet<>(activeJobs);
        if (!activeJob.isBlank()) activeJobs.add(activeJob);
        activeJob = "";
        if (experience == null) experience = new HashMap<>();
        if (completedActions == null) completedActions = new HashMap<>();
        experience.replaceAll((key, value) -> Math.max(0L, value == null ? 0L : value));
        completedActions.replaceAll((key, value) -> Math.max(0L, value == null ? 0L : value));
    }

    public long xp(String job) {
        normalize();
        return experience.getOrDefault(job, 0L);
    }

    public boolean isActive(String job) {
        normalize();
        return activeJobs.contains(job);
    }
}
