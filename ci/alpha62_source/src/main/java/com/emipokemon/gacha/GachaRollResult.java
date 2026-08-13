package com.emipokemon.gacha;

import com.emipokemon.gacha.catalog.PokemonCatalogEntry;

public record GachaRollResult(
        String bannerId,
        PokemonCatalogEntry pokemon,
        GachaTier tier,
        int level,
        boolean shiny,
        boolean epicPityTriggered,
        boolean legendaryPityTriggered
) {
}
