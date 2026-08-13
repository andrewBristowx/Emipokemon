package com.emipokemon.gacha;

public final class GachaProgress {
    public long totalPulls;
    public int pullsSinceEpicOrBetter;
    public int pullsSinceLegendaryOrBetter;
    public String lastSpeciesId;
    public String lastTier;

    public void record(GachaTier tier, String speciesId) {
        totalPulls++;
        lastSpeciesId = speciesId;
        lastTier = tier.name();

        if (tier.isAtLeast(GachaTier.EPIC)) {
            pullsSinceEpicOrBetter = 0;
        } else {
            pullsSinceEpicOrBetter++;
        }

        if (tier.isAtLeast(GachaTier.LEGENDARY)) {
            pullsSinceLegendaryOrBetter = 0;
        } else {
            pullsSinceLegendaryOrBetter++;
        }
    }
}
