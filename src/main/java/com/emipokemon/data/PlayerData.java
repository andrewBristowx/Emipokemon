package com.emipokemon.data;

import java.util.UUID;

public final class PlayerData {
    public int dataVersion = 1;
    public UUID playerId;
    public long firstSeenEpochMillis;
    public long lastSeenEpochMillis;
    public long debugCounter;

    public static PlayerData create(UUID playerId) {
        long now = System.currentTimeMillis();
        PlayerData data = new PlayerData();
        data.playerId = playerId;
        data.firstSeenEpochMillis = now;
        data.lastSeenEpochMillis = now;
        return data;
    }

    public void touch() {
        lastSeenEpochMillis = System.currentTimeMillis();
    }
}
