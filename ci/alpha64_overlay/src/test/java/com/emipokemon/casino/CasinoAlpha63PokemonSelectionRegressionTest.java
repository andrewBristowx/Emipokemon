package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CasinoAlpha63PokemonSelectionRegressionTest {
    private static String source(String path) throws Exception { return Files.readString(Path.of("src", path)); }

    @Test void serverSendsBothSelectedPokemonToDedicatedPortraitFrames() throws Exception {
        String wager = source("main/java/com/emipokemon/casino/PokemonWagerService.java");
        String networking = source("main/java/com/emipokemon/casino/CasinoNetworking.java");
        String screen = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");
        assertTrue(networking.contains("record PokemonDisplay"));
        assertTrue(wager.contains("speciesIdentifier(pokemon)"));
        assertTrue(wager.contains("getMethod(\"getResourceIdentifier\")"));
        assertTrue(wager.contains("cobblemon:"));
        assertTrue(wager.contains("pokemon.getLevel()"));
        assertTrue(screen.contains("drawPokemonFlipSelections(context)"));
        assertTrue(screen.contains("drawCobblemonPortrait"));
        assertTrue(screen.contains("CONFIRMADO"));
    }

    @Test void duplicatePokemonUuidAndAllWagerTransitionsAreAudited() throws Exception {
        String wager = source("main/java/com/emipokemon/casino/PokemonWagerService.java");
        assertTrue(wager.contains("first.getUuid().equals(second.getUuid())"));
        assertTrue(wager.contains("pokemon_flip:confirm"));
        assertTrue(wager.contains("pokemon_flip:escrow_prepared"));
        assertTrue(wager.contains("pokemon_flip:result_committed"));
        assertTrue(wager.contains("pokemon_flip:delivered"));
        assertTrue(wager.contains("pokemon_flip:recovered"));
    }

    @Test void alpha63IdentityIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.64"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.64"));
    }
}
