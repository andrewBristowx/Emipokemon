package com.emipokemon.data;

import com.emipokemon.gacha.GachaProgress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    public int dataVersion = 2;
    public UUID playerId;
    public long firstSeenEpochMillis;
    public long lastSeenEpochMillis;
    public long debugCounter;
    public Map<String, GachaProgress> gachaProgress = new HashMap<>();

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
        dataVersion = 2;
    }

    public GachaProgress gacha(String bannerId) {
        normalize();
        return gachaProgress.computeIfAbsent(bannerId, ignored -> new GachaProgress());
    }

    public void touch() {
        lastSeenEpochMillis = System.currentTimeMillis();
    }
}
