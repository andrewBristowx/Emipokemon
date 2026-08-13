package com.emipokemon.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CasinoSilhouetteRegressionTest {
    private JsonObject model(String id) throws Exception {
        Path path = Path.of("src/main/resources/assets/emipokemon/geo/casino_" + id + ".geo.json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private Map<String, Integer> boneCubeCounts(String id) throws Exception {
        JsonArray bones = model(id).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        Map<String, Integer> counts = new HashMap<>();
        for (var element : bones) {
            JsonObject bone = element.getAsJsonObject();
            counts.put(bone.get("name").getAsString(), bone.has("cubes") ? bone.getAsJsonArray("cubes").size() : 0);
        }
        return counts;
    }

    @Test
    void slotReadsAsAThreeReelLeverMachine() throws Exception {
        Map<String,Integer> bones = boneCubeCounts("slot");
        assertTrue(bones.getOrDefault("root",0) >= 12);
        assertTrue(bones.getOrDefault("reel1",0) >= 2);
        assertTrue(bones.getOrDefault("reel2",0) >= 2);
        assertTrue(bones.getOrDefault("reel3",0) >= 2);
        assertTrue(bones.getOrDefault("lever",0) >= 2);
    }

    @Test
    void exchangeKiosksHaveDifferentFunctionalSilhouettes() throws Exception {
        Map<String,Integer> chip = boneCubeCounts("chip_exchange");
        Map<String,Integer> ticket = boneCubeCounts("ticket_exchange");
        assertTrue(chip.getOrDefault("spinner",0) >= 4, "chip changer needs a visible carousel/hopper");
        assertTrue(ticket.getOrDefault("cards",0) >= 3, "ticket changer needs a protruding ticket strip");
        assertNotEquals(chip, ticket, "chip and ticket kiosks must not be geometry clones");
    }

    @Test
    void rouletteHasARealWheelAssembly() throws Exception {
        Map<String,Integer> bones = boneCubeCounts("roulette");
        assertTrue(bones.getOrDefault("spinner",0) >= 8);
        assertTrue(bones.getOrDefault("ball",0) >= 1);
        assertTrue(bones.getOrDefault("root",0) >= 9);
    }

    @Test
    void tableGamesHaveDistinctGameFurniture() throws Exception {
        Map<String,Integer> poker = boneCubeCounts("poker");
        Map<String,Integer> blackjack = boneCubeCounts("blackjack");
        Map<String,Integer> dice = boneCubeCounts("dice");
        assertTrue(poker.getOrDefault("cards",0) >= 15, "poker must keep five cards plus visible rank/suit detail");
        assertTrue(blackjack.getOrDefault("cards",0) >= 6, "blackjack must keep its card pair plus rank/suit detail");
        assertTrue(dice.getOrDefault("dice",0) >= 14, "craps table needs two dice plus physical pips");
        assertNotEquals(poker.get("root"), blackjack.get("root"));
        assertNotEquals(blackjack.get("root"), dice.get("root"));
    }

    @Test
    void allAnimatedBoneNamesRemainCompatible() throws Exception {
        assertTrue(boneCubeCounts("slot").keySet().containsAll(Set.of("reel1","reel2","reel3","lever","lights")));
        assertTrue(boneCubeCounts("chip_exchange").keySet().containsAll(Set.of("spinner","lights")));
        assertTrue(boneCubeCounts("ticket_exchange").keySet().containsAll(Set.of("cards","lights")));
        assertTrue(boneCubeCounts("roulette").keySet().containsAll(Set.of("spinner","ball","lights")));
        assertTrue(boneCubeCounts("poker").keySet().containsAll(Set.of("cards","lights")));
        assertTrue(boneCubeCounts("blackjack").keySet().containsAll(Set.of("cards","lights")));
        assertTrue(boneCubeCounts("dice").keySet().containsAll(Set.of("dice","lights")));
    }

    @Test
    void alpha60VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.62"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.62"));
    }
}

