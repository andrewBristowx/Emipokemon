package com.emipokemon.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CasinoConstructionDetailRegressionTest {
    private JsonArray bones(String id) throws Exception {
        JsonObject root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/emipokemon/geo/casino_"+id+".geo.json"))).getAsJsonObject();
        return root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
    }

    private Map<String,Integer> counts(String id) throws Exception {
        Map<String,Integer> out=new HashMap<>();
        for (var e:bones(id)) {
            JsonObject b=e.getAsJsonObject();
            out.put(b.get("name").getAsString(),b.has("cubes")?b.getAsJsonArray("cubes").size():0);
        }
        return out;
    }

    private double maxY(String id) throws Exception {
        double max=0;
        for (var e:bones(id)) {
            JsonObject b=e.getAsJsonObject();
            if (!b.has("cubes")) continue;
            for (var ce:b.getAsJsonArray("cubes")) {
                JsonObject c=ce.getAsJsonObject();
                double y=c.getAsJsonArray("origin").get(1).getAsDouble();
                double sy=c.getAsJsonArray("size").get(1).getAsDouble();
                max=Math.max(max,y+sy);
            }
        }
        return max;
    }

    @Test
    void verticalMachinesReachTwoBlockPresenceWithoutOvershooting() throws Exception {
        for (String id:List.of("slot","chip_exchange","ticket_exchange")) {
            assertTrue(maxY(id)>=31.8,id+" should visually occupy two blocks");
            assertTrue(maxY(id)<=32.0,id+" must never exceed two blocks");
            assertTrue(counts(id).getOrDefault("root",0)>=18,id+" needs layered cabinet construction detail");
        }
    }

    @Test
    void tableGamesHaveEnoughGameSpecificHardware() throws Exception {
        assertTrue(counts("roulette").getOrDefault("root",0)>=16,"roulette needs wheel plus betting board furniture");
        assertTrue(counts("poker").getOrDefault("root",0)>=15,"poker needs chips, button and betting spots");
        assertTrue(counts("blackjack").getOrDefault("root",0)>=18,"blackjack needs player pads, shoe and discard tray");
        assertTrue(counts("dice").getOrDefault("root",0)>=17,"dice table needs marked proposition zones and rails");
    }

    @Test
    void animatedBonesRemainAvailable() throws Exception {
        assertTrue(counts("slot").keySet().containsAll(List.of("reel1","reel2","reel3","lever","lights")));
        assertTrue(counts("chip_exchange").containsKey("spinner"));
        assertTrue(counts("ticket_exchange").containsKey("cards"));
        assertTrue(counts("roulette").keySet().containsAll(List.of("spinner","ball","lights")));
        assertTrue(counts("poker").containsKey("cards"));
        assertTrue(counts("blackjack").containsKey("cards"));
        assertTrue(counts("dice").containsKey("dice"));
    }

    @Test
    void alpha60VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.64"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.64"));
    }
}
