from pathlib import Path

root=Path('.')

def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text)

write('src/test/java/com/emipokemon/casino/CasinoMultiplayerRegressionTest.java', r'''package com.emipokemon.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CasinoMultiplayerRegressionTest {
    private String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void alpha36UsesServerAuthoritativeSharedTableSessions() throws Exception {
        String table = source("main/java/com/emipokemon/casino/CasinoTableService.java");
        String networking = source("main/java/com/emipokemon/casino/CasinoNetworking.java");
        String legacy = source("main/java/com/emipokemon/casino/CasinoService.java");
        assertTrue(table.contains("CasinoGameType.ROULETTE"));
        assertTrue(table.contains("CasinoGameType.DICE"));
        assertTrue(table.contains("CasinoGameType.BLACKJACK"));
        assertTrue(table.contains("CasinoGameType.POKER"));
        assertTrue(table.contains("RANDOM.nextInt(37)"));
        assertTrue(table.contains("Dealer"));
        assertTrue(table.contains("POKER_FLOP"));
        assertTrue(table.contains("refund(participant.id"));
        assertTrue(networking.contains("TABLES.action(player, machine"));
        assertTrue(networking.contains("TABLES.open(player, machine"));
        assertTrue(legacy.contains("sesión multijugador compartida"));
        assertFalse(legacy.contains("case ROULETTE -> roulette(player"));
    }

    @Test
    void alpha36HasOfflineSafeCasinoPayoutCredit() throws Exception {
        String progression = source("main/java/com/emipokemon/progress/ProgressionService.java");
        assertTrue(progression.contains("refund(UUID playerId"));
        assertTrue(progression.contains("dataManager.getOrLoad(playerId)"));
        assertTrue(progression.contains("dataManager.saveNow(playerId)"));
        assertTrue(progression.contains("audit(playerId, amount"));
    }

    @Test
    void alpha36ClientExposesSharedBlackjackAndPokerActions() throws Exception {
        String screen = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");
        assertTrue(screen.contains("Unirse a la mano"));
        assertTrue(screen.contains("Pedir"));
        assertTrue(screen.contains("Plantarse"));
        assertTrue(screen.contains("Entrar al bote"));
        assertTrue(screen.contains("Retirarse"));
        assertTrue(screen.contains("state.deadlineMillis()"));
    }

    @Test
    void alpha36CasinoModelsStayInsideOneMinecraftBlock() throws Exception {
        List<String> models = List.of("slot", "chip_exchange", "ticket_exchange", "roulette", "poker", "blackjack", "dice");
        for (String model : models) {
            Path path = Path.of("src/main/resources/assets/emipokemon/geo/casino_" + model + ".geo.json");
            assertTrue(Files.isRegularFile(path), model);
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonArray bones = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
            for (var boneElement : bones) {
                JsonObject bone = boneElement.getAsJsonObject();
                if (!bone.has("cubes")) continue;
                for (var cubeElement : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = cubeElement.getAsJsonObject();
                    JsonArray origin = cube.getAsJsonArray("origin");
                    JsonArray size = cube.getAsJsonArray("size");
                    double x = origin.get(0).getAsDouble(), y = origin.get(1).getAsDouble(), z = origin.get(2).getAsDouble();
                    double sx = size.get(0).getAsDouble(), sy = size.get(1).getAsDouble(), sz = size.get(2).getAsDouble();
                    assertTrue(x >= -8.0 && x + sx <= 8.0, model + " x bounds: " + cube);
                    assertTrue(y >= 0.0 && y + sy <= 16.0, model + " y bounds: " + cube);
                    assertTrue(z >= -8.0 && z + sz <= 8.0, model + " z bounds: " + cube);
                }
            }
            Path texture = Path.of("src/main/resources/assets/emipokemon/textures/block/casino_" + model + ".png");
            byte[] png = Files.readAllBytes(texture);
            assertTrue(png.length > 1000, model + " texture too small");
            assertArrayEquals(new byte[]{(byte)137,80,78,71,13,10,26,10}, java.util.Arrays.copyOf(png, 8), model);
        }
    }

    @Test
    void alpha36VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.36"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.36"));
    }
}
''')
