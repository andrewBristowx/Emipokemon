package com.emipokemon.gacha.catalog;

import com.emipokemon.gacha.GachaTier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record PokemonCatalogEntry(
        String speciesId,
        String displayName,
        int nationalDex,
        int generation,
        String region,
        Set<String> types,
        Set<String> labels,
        int baseStatTotal,
        int catchRate,
        GachaTier tier
) {
    public PokemonCatalogEntry {
        types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
        labels = Collections.unmodifiableSet(new LinkedHashSet<>(labels));
    }

    public boolean hasType(String type) {
        return types.contains(type.toLowerCase());
    }

    public boolean hasLabel(String label) {
        return labels.contains(label.toLowerCase());
    }
}
