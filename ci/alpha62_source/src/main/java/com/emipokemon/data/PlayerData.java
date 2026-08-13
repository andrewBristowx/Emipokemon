package com.emipokemon.data;

import com.emipokemon.gacha.GachaProgress;
import com.emipokemon.progress.data.EconomyProgress;
import com.emipokemon.progress.data.JobProgress;
import com.emipokemon.progress.data.QuestProgress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {
    public int dataVersion = 5;
    public UUID playerId;
    public long firstSeenEpochMillis;
    public long lastSeenEpochMillis;
    public long debugCounter;
    public Map<String, GachaProgress> gachaProgress = new HashMap<>();
    public EconomyProgress economy = new EconomyProgress();
    public JobProgress jobs = new JobProgress();
    public QuestProgress quests = new QuestProgress();
    public Set<String> claimedNpcRewards = new HashSet<>();

    public static PlayerData create(UUID playerId) {
        long now = System.currentTimeMillis();
        PlayerData data = new PlayerData();
        data.playerId = playerId;
        data.firstSeenEpochMillis = now;
        data.lastSeenEpochMillis = now;
        data.normalize();
        return data;
    }

    public void normalize() {
        if (gachaProgress == null) gachaProgress = new HashMap<>();
        if (economy == null) economy = new EconomyProgress();
        if (jobs == null) jobs = new JobProgress();
        if (quests == null) quests = new QuestProgress();
        if (claimedNpcRewards == null) claimedNpcRewards = new HashSet<>();
        economy.normalize();
        jobs.normalize();
        quests.normalize();
        dataVersion = 5;
    }

    public GachaProgress gacha(String bannerId) {
        normalize();
        return gachaProgress.computeIfAbsent(bannerId, ignored -> new GachaProgress());
    }

    public void touch() {
        lastSeenEpochMillis = System.currentTimeMillis();
    }
}
