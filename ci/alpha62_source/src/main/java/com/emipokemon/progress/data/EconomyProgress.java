package com.emipokemon.progress.data;

public final class EconomyProgress {
    public long michicoins;
    public long lifetimeEarned;
    public long lifetimeSpent;
    public long earnedToday;
    public String dailyKey = "";
    public long activeSecondsBank;

    public void normalize() {
        michicoins = Math.max(0L, michicoins);
        lifetimeEarned = Math.max(0L, lifetimeEarned);
        lifetimeSpent = Math.max(0L, lifetimeSpent);
        earnedToday = Math.max(0L, earnedToday);
        activeSecondsBank = Math.max(0L, activeSecondsBank);
        if (dailyKey == null) dailyKey = "";
    }
}
