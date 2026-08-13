package com.emipokemon.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CobbleverseResourcesTest {
    @Test
    void rocketAdvancementUsesExactRctTrainerId() {
        var stream = getClass().getResourceAsStream(
                "/data/emipokemon/advancement/integration/defeat_team_rocket_giovanni.json");
        assertNotNull(stream);
        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject criterion = root.getAsJsonObject("criteria")
                .getAsJsonObject("team_rocket_giovanni_defeated");
        assertEquals("rctmod:defeat_count", criterion.get("trigger").getAsString());
        assertEquals("team_rocket_giovanni", criterion.getAsJsonObject("conditions")
                .getAsJsonArray("trainer_ids").get(0).getAsString());
    }

    @Test
    void serverMixinIsPackaged() {
        var stream = getClass().getResourceAsStream("/emipokemon.mixins.json");
        assertNotNull(stream);
        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(root.getAsJsonArray("mixins").toString().contains("LumyMonSummonAltarMixin"));
    }
}
