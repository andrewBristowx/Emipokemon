package com.emipokemon.data;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerDataMigrationTest {
    @Test
    void alpha16DataNormalizesWithoutLosingProgress() {
        UUID playerId = UUID.fromString("5eb79c7a-e17f-4a06-bf08-f154217f187a");
        String alpha16 = """
                {
                  "dataVersion": 4,
                  "playerId": "5eb79c7a-e17f-4a06-bf08-f154217f187a",
                  "firstSeenEpochMillis": 100,
                  "lastSeenEpochMillis": 200,
                  "economy": {"michicoins": 1234},
                  "quests": {
                    "objectives": {"starter": 1},
                    "completed": ["starter"],
                    "claimed": ["starter"],
                    "discoveredSpecies": ["pikachu"],
                    "discoveredBiomes": ["minecraft:plains"],
                    "battleWinStreak": 2
                  }
                }
                """;

        PlayerData data = new Gson().fromJson(alpha16, PlayerData.class);
        data.normalize();

        assertEquals(playerId, data.playerId);
        assertEquals(5, data.dataVersion);
        assertEquals(1234, data.economy.michicoins);
        assertTrue(data.quests.claimed.contains("starter"));
        assertTrue(data.quests.discoveredSpecies.contains("pikachu"));
        assertNotNull(data.gachaProgress);
        assertNotNull(data.jobs);
        assertNotNull(data.claimedNpcRewards);
    }
}
