package com.emipokemon.progress.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class QuestProgress {
    public Map<String, Long> objectives = new HashMap<>();
    public Set<String> completed = new HashSet<>();
    public Set<String> claimed = new HashSet<>();
    public Set<String> discoveredSpecies = new HashSet<>();
    public Set<String> discoveredBiomes = new HashSet<>();
    public long battleWinStreak;

    public void normalize() {
        if (objectives == null) objectives = new HashMap<>();
        if (completed == null) completed = new HashSet<>();
        if (claimed == null) claimed = new HashSet<>();
        if (discoveredSpecies == null) discoveredSpecies = new HashSet<>();
        if (discoveredBiomes == null) discoveredBiomes = new HashSet<>();
        objectives.replaceAll((key, value) -> Math.max(0L, value == null ? 0L : value));
        battleWinStreak = Math.max(0L, battleWinStreak);
    }
}
