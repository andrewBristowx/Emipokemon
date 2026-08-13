package com.emipokemon.gacha;

public enum GachaTier {
    COMMON(0),
    UNCOMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4),
    MYTHICAL(5),
    SPECIAL(6);

    private final int rank;

    GachaTier(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean isAtLeast(GachaTier other) {
        return this.rank >= other.rank;
    }

    public static GachaTier parse(String value, GachaTier fallback) {
        if (value == null) return fallback;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
