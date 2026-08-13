package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CasinoAlpha61EffectsRegressionTest {
    private static String clientSource(String name) throws Exception {
        return Files.readString(Path.of("src/client/java/com/emipokemon/client/casino/" + name));
    }

    @Test
    void presentationTimelineSurvivesServerScreenRefreshes() throws Exception {
        String client = clientSource("CasinoClient.java");
        String screen = clientSource("CasinoScreen.java");
        assertTrue(client.contains("casino.presentationState()"));
        assertTrue(client.contains("new CasinoScreen(parent, json, previousAmount, presentation)"));
        assertTrue(screen.contains("previousPresentation.signature.equals(signature)"));
        assertTrue(screen.contains("this.openedAt = presentation.startedAt"));
    }

    @Test
    void diceAnimationUsesTheAuthoritativePublicResult() throws Exception {
        String screen = clientSource("CasinoScreen.java");
        String service = Files.readString(Path.of("src/main/java/com/emipokemon/casino/CasinoTableService.java"));
        assertTrue(service.contains("Dados compartidos: \" + first + \" + \" + second"));
        assertTrue(screen.contains("safe(state.tableState(), \"\") + \" \" + safe(state.message(), \"\")"));
        assertTrue(screen.contains("\"result\".equals(state.phase()) && hasDiceResult() && elapsed < 2600L"));
        assertTrue(screen.contains("diceValues()"));
    }

    @Test
    void casinoEffectsAreLocalAndRateLimited() throws Exception {
        String screen = clientSource("CasinoScreen.java");
        assertTrue(screen.contains("PositionedSoundInstance.master(sound, pitch)"));
        assertTrue(screen.contains("step == presentation.lastTimedStep"));
        assertTrue(screen.contains("presentation.resultSoundPlayed"));
        assertTrue(screen.contains("SoundEvents.ITEM_BOOK_PAGE_TURN"));
        assertTrue(screen.contains("SoundEvents.BLOCK_DISPENSER_LAUNCH"));
        assertTrue(screen.contains("SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP"));
        assertTrue(screen.contains("ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload"));
    }

    @Test
    void alpha61VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.64"));
        assertTrue(Files.readString(Path.of("src/main/java/com/emipokemon/Emipokemon.java")).contains("0.4.0-alpha.64"));
    }
}
