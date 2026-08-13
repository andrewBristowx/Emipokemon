package com.emipokemon.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CasinoSettingsTest {
    @Test
    void unsafeCasinoValuesAreNormalized() {
        EmipokemonConfig.CasinoSettings settings = new EmipokemonConfig.CasinoSettings();
        settings.minimumBet = -1;
        settings.maximumBet = 0;
        settings.maximumPayout = 0;
        settings.chipPrice = 0;
        settings.normalTicketPrice = Long.MAX_VALUE;
        settings.slotPayoutMultiplier = Double.NaN;
        settings.pokerPayoutMultiplier = 200.0D;

        settings.normalize();

        assertEquals(1L, settings.minimumBet);
        assertEquals(1L, settings.maximumBet);
        assertEquals(1L, settings.maximumPayout);
        assertEquals(1L, settings.chipPrice);
        assertEquals(100_000_000L, settings.normalTicketPrice);
        assertEquals(1.0D, settings.slotPayoutMultiplier);
        assertEquals(10.0D, settings.pokerPayoutMultiplier);
    }
}
