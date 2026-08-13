package com.emipokemon.rewards.data;

public final class DailyRewardProgress {
    public String lastClaimDate = "";
    public int streak;
    public int totalClaims;
    public String lastRewardLabel = "";

    public void normalize() {
        if (lastClaimDate == null) lastClaimDate = "";
        if (lastRewardLabel == null) lastRewardLabel = "";
        streak = Math.max(0, streak);
        totalClaims = Math.max(0, totalClaims);
    }
}
