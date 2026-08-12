package com.emipokemon.alpha69;

import com.emipokemon.gacha.banner.FeaturedRotationService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

final class Alpha69GachaFixesRegressionTest {
    private static String source(String path) throws Exception { return Files.readString(Path.of("src", path)); }

    @Test void emiLegendaryRotationUsesTheCompleteLegendaryCatalogAndTwelveHourWindows() throws Exception {
        String rotation = source("main/java/com/emipokemon/gacha/banner/FeaturedRotationService.java");
        assertEquals(12L * 60L * 60L * 1000L, FeaturedRotationService.EMI_ROTATION_MILLIS);
        assertEquals(0L, FeaturedRotationService.rotationBucket(0L));
        assertEquals(0L, FeaturedRotationService.rotationBucket(FeaturedRotationService.EMI_ROTATION_MILLIS - 1L));
        assertEquals(1L, FeaturedRotationService.rotationBucket(FeaturedRotationService.EMI_ROTATION_MILLIS));
        assertTrue(rotation.contains("entry.tier() == GachaTier.LEGENDARY"));
        assertTrue(rotation.contains("ThreadLocalRandom.current().nextInt"));
        assertTrue(rotation.contains("emi_featured_rotation.json"));
        assertTrue(rotation.contains("choices.removeIf"));
    }

    @Test void emiFeaturedLegendaryAffectsRealServerRollsWithoutChangingNormalRandomRolls() throws Exception {
        String service = source("main/java/com/emipokemon/gacha/GachaService.java");
        String network = source("main/java/com/emipokemon/gacha/GachaNetworking.java");
        String machine = source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java");
        assertTrue(service.contains("EMI_FEATURED_MULTIPLIER"));
        assertTrue(service.contains("machineOverride"));
        assertTrue(service.contains("featuredOverride.tier() == GachaTier.LEGENDARY"));
        assertTrue(network.contains("machine.isEmiThemed() ? machine.getFeaturedSpeciesId() : \"\""));
        assertTrue(machine.contains("isEmiThemed() ? featuredSpeciesId : \"\""));
        assertTrue(machine.contains("currentStandardSpotlight"));
        assertTrue(machine.contains("bannerId = \"standard\""));
    }

    @Test void x10ResultsRevealSequentiallyWithSoundAndPortraitClipping() throws Exception {
        String screen = source("client/java/com/emipokemon/client/gacha/GachaScreen.java");
        assertTrue(screen.contains("RESULT_STAGGER_MS = 135L"));
        assertTrue(screen.contains("resultPopScale"));
        assertTrue(screen.contains("visibleResultCount"));
        assertTrue(screen.contains("enableScissor"));
        assertTrue(screen.contains("disableScissor"));
        assertTrue(screen.contains("PositionedSoundInstance.master"));
        assertTrue(screen.contains("UI_TOAST_CHALLENGE_COMPLETE"));
        assertTrue(screen.contains("BLOCK_AMETHYST_BLOCK_CHIME"));
    }

    @Test void seasonalWorldDisplayNoLongerUsesGuiProfileRendererOrMutatedCameraRotation() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/SeasonalPokemonWorldRenderer.java");
        assertFalse(renderer.contains("drawProfilePokemon"));
        assertTrue(renderer.contains("CobblemonEntities.POKEMON.create"));
        assertTrue(renderer.contains("getEntityRenderDispatcher().render"));
        assertTrue(renderer.contains("new Quaternionf(client.getEntityRenderDispatcher().getRotation())"));
        assertTrue(renderer.contains("1.45F / width"));
        assertTrue(renderer.contains("1.35F / height"));
    }

    @Test void physicalMachineUsesNoCullTranslucencyWithoutChangingApprovedTextures() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/GachaMachineRenderer.java");
        assertTrue(renderer.contains("RenderLayer.getEntityTranslucent(texture)"));
        assertFalse(renderer.contains("RenderLayer.getEntityTranslucentCull(texture)"));
        assertEquals("42d060134c7809f2617bcf1aa7a06f02750908eb616fcbbfb3b837dbf98e30cd",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/block/standard_gacha_machine.png")));
        assertEquals("4d0d76caca51f7d167d3197030d7074e9ac2b7e1ade015c5a01c462ef3e4aea0",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/block/emi_gacha_machine.png")));
    }

    @Test void approvedGachaBackgroundsRemainByteIdentical() throws Exception {
        assertEquals("694aadcaca9a8dcaace9c26a9e43cf41e056203855cd380670d0b7c208f4d98d",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/standard_gacha.png")));
        assertEquals("4c61d6a00e324a82d060b8c7b9a3abe40daa437fda8d8efb5a4eaf45b973e216",
                sha256(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/emi_gacha.png")));
        assertFalse(Files.exists(Path.of("src/main/resources/assets/emipokemon/textures/gui/gacha/reveal_sheet.png")));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
