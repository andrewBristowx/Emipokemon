package com.emipokemon.rewards;

import java.util.ArrayList;
import java.util.List;

public final class BattlePassSnapshot {
    public String playerName = "";
    public long experience;
    public int level;
    public long levelStartXp;
    public long nextLevelXp;
    public boolean premium;
    public int page;
    public int pageSize = 8;
    public long standardRolls;
    public long emiRolls;
    public String message = "";
    public List<RewardSlot> free = new ArrayList<>();
    public List<RewardSlot> premiumTrack = new ArrayList<>();

    public static final class RewardSlot {
        public int level;
        public String type = "MILESTONE";
        public int amount;
        public boolean unlocked;
        public boolean claimed;
        public boolean claimable;
        public String label = "Progreso";
    }
}
