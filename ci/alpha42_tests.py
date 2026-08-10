from pathlib import Path

root=Path('.')

# Advance inherited alpha.40 version assertions without changing gameplay expectations.
for rel in [
 'src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoVisualRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoSilhouetteRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoMaterialScaleRegressionTest.java',
 'src/test/java/com/emipokemon/casino/CasinoConstructionDetailRegressionTest.java',
]:
    p=root/rel
    if p.exists():
        s=p.read_text().replace('0.4.0-alpha.40','0.4.0-alpha.42')
        s=s.replace('alpha40VersionIsConsistentInSource','alpha42VersionIsConsistentInSource')
        s=s.replace('alpha40VersionIsConsistent','alpha42VersionIsConsistent')
        # alpha.42 deliberately adds rank/suit and pip geometry to the existing animated bones.
        s=s.replace('assertEquals(5, poker.getOrDefault("cards",0), "poker must visibly expose the five-card community lane");',
                    'assertTrue(poker.getOrDefault("cards",0) >= 15, "poker must keep five cards plus visible rank/suit detail");')
        s=s.replace('assertEquals(2, blackjack.getOrDefault("cards",0), "blackjack should show a dealer/player card pair, not a poker board");',
                    'assertTrue(blackjack.getOrDefault("cards",0) >= 6, "blackjack must keep its card pair plus rank/suit detail");')
        s=s.replace('assertEquals(2, dice.getOrDefault("dice",0), "craps table needs two visible dice");',
                    'assertTrue(dice.getOrDefault("dice",0) >= 14, "craps table needs two dice plus physical pips");')
        p.write_text(s)

p=root/'src/test/java/com/emipokemon/casino/CasinoConnectedPiecesRegressionTest.java'
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(r'''package com.emipokemon.casino;

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

class CasinoConnectedPiecesRegressionTest {
    private JsonArray bones(String id) throws Exception {
        JsonObject root=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/emipokemon/geo/casino_"+id+".geo.json"))).getAsJsonObject();
        return root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
    }

    private Map<String,JsonObject> map(String id) throws Exception {
        Map<String,JsonObject> out=new HashMap<>();
        for (var e:bones(id)) {
            JsonObject b=e.getAsJsonObject();
            out.put(b.get("name").getAsString(),b);
        }
        return out;
    }

    private int cubes(JsonObject bone) {
        return bone!=null && bone.has("cubes") ? bone.getAsJsonArray("cubes").size() : 0;
    }

    private boolean hasCube(String id,double ox,double oy,double oz,double sx,double sy,double sz) throws Exception {
        for (var e:bones(id)) {
            JsonObject b=e.getAsJsonObject();
            if (!b.has("cubes")) continue;
            for (var ce:b.getAsJsonArray("cubes")) {
                JsonObject c=ce.getAsJsonObject();
                JsonArray o=c.getAsJsonArray("origin"), s=c.getAsJsonArray("size");
                if (Math.abs(o.get(0).getAsDouble()-ox)<0.001 && Math.abs(o.get(1).getAsDouble()-oy)<0.001 && Math.abs(o.get(2).getAsDouble()-oz)<0.001 &&
                    Math.abs(s.get(0).getAsDouble()-sx)<0.001 && Math.abs(s.get(1).getAsDouble()-sy)<0.001 && Math.abs(s.get(2).getAsDouble()-sz)<0.001) return true;
            }
        }
        return false;
    }

    @Test
    void tallCabinetsHaveContinuousRearShells() throws Exception {
        for (String id:List.of("slot","chip_exchange","ticket_exchange")) {
            assertTrue(hasCube(id,-5.55,1.2,3.25,11.1,30.15,0.72),id+" needs a continuous rear shell");
            assertTrue(hasCube(id,-5.85,1.0,3.32,0.65,30.7,0.78),id+" needs left structural rib");
            assertTrue(hasCube(id,5.20,1.0,3.32,0.65,30.7,0.78),id+" needs right structural rib");
        }
    }

    @Test
    void cardsHavePhysicalMarksInsteadOfBlankSlabs() throws Exception {
        Map<String,JsonObject> poker=map("poker");
        Map<String,JsonObject> blackjack=map("blackjack");
        assertTrue(cubes(poker.get("cards"))>=15,"five poker cards need rank/suit geometry");
        assertTrue(cubes(blackjack.get("cards"))>=6,"blackjack cards need rank/suit geometry");
    }

    @Test
    void diceHavePhysicalPipsAndKeepAnimatedBone() throws Exception {
        Map<String,JsonObject> dice=map("dice");
        assertTrue(dice.containsKey("dice"));
        assertTrue(cubes(dice.get("dice"))>=14,"two dice should include visible top/front pip geometry");
    }

    @Test
    void blankBettingAreasReceiveCenterMarks() throws Exception {
        assertTrue(cubes(map("roulette").get("root"))>=22);
        assertTrue(cubes(map("blackjack").get("root"))>=23);
        assertTrue(cubes(map("dice").get("root"))>=23);
    }

    @Test
    void alpha42VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.42"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.42"));
    }
}
''')
