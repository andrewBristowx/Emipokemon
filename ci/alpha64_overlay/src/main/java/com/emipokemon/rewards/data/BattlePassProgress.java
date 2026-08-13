package com.emipokemon.rewards.data;

import java.util.HashSet;
import java.util.Set;

public final class BattlePassProgress {
    public long experience;
    public int activeSecondsBank;
    public Set<Integer> claimedFree = new HashSet<>();
    public Set<Integer> claimedPremium = new HashSet<>();

    public void normalize() {
        experience = Math.max(0L, experience);
        activeSecondsBank = Math.max(0, activeSecondsBank);
        if (claimedFree == null) claimedFree = new HashSet<>();
        if (claimedPremium == null) claimedPremium = new HashSet<>();
        claimedFree.removeIf(level -> level == null || level < 1);
        claimedPremium.removeIf(level -> level == null || level < 1);
    }
}
