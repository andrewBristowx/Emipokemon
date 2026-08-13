package com.emipokemon.rewards;

import java.util.ArrayList;
import java.util.List;

public final class DailyRewardSnapshot {
    public boolean eligible;
    public long nextClaimEpochMillis;
    public int streak;
    public int totalClaims;
    public String lastReward = "";
    public String message = "";
    public RewardView revealed;
    public List<RewardView> possibleRewards = new ArrayList<>();

    public static final class RewardView {
        public String type = "";
        public String value = "";
        public String label = "";
        public int amount;
        public int weight;
        public String speciesId = "";
        public int level;
        public boolean shiny;
    }
}
