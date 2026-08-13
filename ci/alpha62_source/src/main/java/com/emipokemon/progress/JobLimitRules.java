package com.emipokemon.progress;

import java.util.Locale;
import java.util.Set;

/** Pure rank-to-slot rules, kept separate so migrations and permissions are testable. */
public final class JobLimitRules {
    private static final int DEFAULT_LIMIT = 2;
    private static final int SUPPORTER_LIMIT = 4;
    private static final Set<String> SUPPORTER_GROUPS = Set.of("michivip", "michidonador");
    private static final Set<String> STAFF_GROUPS = Set.of("michimod", "michidueña", "michiduena");

    private JobLimitRules() {
    }

    public static int forGroups(Iterable<String> groups, int allJobs) {
        int result = DEFAULT_LIMIT;
        if (groups == null) return Math.min(result, allJobs);
        for (String group : groups) {
            String normalized = group == null ? "" : group.toLowerCase(Locale.ROOT);
            if (STAFF_GROUPS.contains(normalized)) return allJobs;
            if (SUPPORTER_GROUPS.contains(normalized)) result = Math.max(result, SUPPORTER_LIMIT);
        }
        return Math.min(result, allJobs);
    }
}
