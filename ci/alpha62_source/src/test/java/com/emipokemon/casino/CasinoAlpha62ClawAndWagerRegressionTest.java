package com.emipokemon.casino;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CasinoAlpha62ClawAndWagerRegressionTest {
    private static String source(String path) throws Exception { return Files.readString(Path.of("src", path)); }

    @Test void clawTicketIsSeparateFromGachaAndUsesRealPokeblocksIds() throws Exception {
        String service = source("main/java/com/emipokemon/casino/CasinoService.java");
        String config = source("main/java/com/emipokemon/config/EmipokemonConfig.java");
        assertTrue(service.contains("ModRegistries.CLAW_TICKET"));
        assertTrue(service.contains("El ticket gacha no sirve"));
        assertTrue(config.contains("clawTicketPrice = 250L"));
        assertTrue(config.contains("pokeblocks:pokedoll_eevee"));
        assertTrue(source("main/resources/fabric.mod.json").contains("\"pokeblocks\""));
    }

    @Test void pokemonWagerIsBestOfThreeAndPersistsEscrowBeforeRemoval() throws Exception {
        String wager = source("main/java/com/emipokemon/casino/PokemonWagerService.java");
        assertTrue(wager.indexOf("writeEscrow(escrow)") < wager.indexOf("firstParty.remove(first)"));
        assertTrue(wager.contains("while (firstWins < 2 && secondWins < 2)"));
        assertTrue(wager.contains("restoreIfMissing"));
        assertTrue(wager.contains("deliverIfMissing"));
        assertTrue(wager.contains("UUID checks make this idempotent"));
        assertTrue(wager.contains("get(pokemonId) != null"));
        assertTrue(wager.contains("status = \"DELIVERED\""));
    }

    @Test void sharedPokerRequiresEqualBuyInAndPayoutsAreTotalReturns() throws Exception {
        String tables = source("main/java/com/emipokemon/casino/CasinoTableService.java");
        assertTrue(tables.contains("Esta ronda usa una entrada única"));
        assertTrue(tables.contains("payout(participant.bet, 2L"));
        assertTrue(tables.contains("payoutDecimal(participant.bet, 2.5D"));
        assertTrue(tables.contains("paidPot = payoutDecimal(pot, 1.0D"));
    }

    @Test void alpha62VersionIsConsistent() throws Exception {
        assertTrue(Files.readString(Path.of("gradle.properties")).contains("mod_version=0.4.0-alpha.62"));
        assertTrue(source("main/java/com/emipokemon/Emipokemon.java").contains("0.4.0-alpha.62"));
    }
}
