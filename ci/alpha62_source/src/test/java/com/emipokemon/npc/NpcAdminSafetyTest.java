package com.emipokemon.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcAdminSafetyTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void npcIdIsTrackedAndUploadedFilesRemainServerAuthoritative() throws Exception {
        String entity = source("main/java/com/emipokemon/npc/ServiceNpcEntity.java");
        String network = source("main/java/com/emipokemon/npc/NpcNetworking.java");
        assertTrue(entity.contains("TrackedData<String> NPC_ID"));
        assertTrue(entity.contains("builder.add(NPC_ID, \"\")"));
        assertTrue(network.contains("player.hasPermissionLevel(4)"));
        assertTrue(network.contains("VisualAssetService.MAX_BYTES"));
        assertTrue(network.contains("validTarget(player.getServer(), kind, id)"));
        assertTrue(network.contains("activeForPlayer >= 2"));
    }

    @Test
    void battlesRequireExactRctApiAndNearbyNpc() throws Exception {
        String battle = source("main/java/com/emipokemon/npc/NpcBattleService.java");
        assertTrue(battle.contains("getInstance\", String.class).invoke(null, \"rctmod\")"));
        assertTrue(battle.contains("getMethod(\"startSingle\", trainerClass, trainerClass)"));
        assertTrue(battle.contains("player.squaredDistanceTo(npc) > 64.0D"));
        assertTrue(battle.contains("PokemonSpecies.getByName(properties.getSpecies())"));
        assertFalse(battle.contains("executeWithPrefix"));
    }
}
