package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CasinoAlpha63InteractiveClawRegressionTest {
    private static String source(String path) throws Exception { return Files.readString(Path.of("src", path)); }

    @Test void clawUsesFiveServerAuthoritativeLanesAndRealItemIds() throws Exception {
        String claw = source("main/java/com/emipokemon/casino/ClawGameService.java");
        assertTrue(claw.contains("LANE_COUNT = 5"));
        assertTrue(claw.contains("claw_left"));
        assertTrue(claw.contains("claw_right"));
        assertTrue(claw.contains("claw_drop"));
        assertTrue(claw.contains("\"pokeblocks\".equals(id.getNamespace())"));
        assertTrue(claw.contains("id.getPath().startsWith(\"pokedoll_\")"));
        assertTrue(claw.contains("operation.status = \"PREPARED\""));
        assertTrue(claw.indexOf("writeOperation(operation)") < claw.indexOf("removeOne(player, ModRegistries.CLAW_TICKET)"));
        assertTrue(claw.contains("deliver(player, new ItemStack(ModRegistries.CLAW_TICKET))"));
        assertTrue(claw.indexOf("writeOperation(operation)") < claw.indexOf("deliver(player, new ItemStack(prize))"));
    }

    @Test void clawScreenRendersRegistryItemsInsteadOfPaintedCreatures() throws Exception {
        String screen = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");
        assertTrue(screen.contains("drawInteractiveClaw(context)"));
        assertTrue(screen.contains("Registries.ITEM.get(id)"));
        assertTrue(screen.contains("context.drawItem(stack, 0, 0)"));
        assertTrue(screen.contains("Bajar garra"));
    }

    @Test void clawTicketHasRealTransparencyAndReadableResolution() throws Exception {
        var image = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/item/claw_ticket.png").toFile());
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        boolean transparent = false;
        boolean opaque = false;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int alpha = image.getRGB(x, y) >>> 24;
            transparent |= alpha == 0;
            opaque |= alpha > 240;
        }
        assertTrue(transparent && opaque);
    }

    @Test void worldModelsHaveDistinctClawAndCoinMechanisms() throws Exception {
        String claw = source("main/resources/assets/emipokemon/geo/casino_claw.geo.json");
        String flip = source("main/resources/assets/emipokemon/geo/casino_pokemon_flip.geo.json");
        String block = source("main/java/com/emipokemon/casino/CasinoMachineBlock.java");
        assertTrue(claw.contains("\"claw_carriage\""));
        assertTrue(claw.contains("\"claw\""));
        assertTrue(flip.contains("\"spinner\""));
        assertTrue(flip.contains("\"coin\""));
        assertTrue(block.contains("type == CasinoGameType.CLAW) return CLAW_SHAPE"));
        assertTrue(block.contains("type == CasinoGameType.POKEMON_FLIP ? TABLE_SHAPE"));
    }
}
