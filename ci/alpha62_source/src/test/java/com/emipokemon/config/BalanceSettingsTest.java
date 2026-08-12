package com.emipokemon.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceSettingsTest {
    @Test
    void balanceValuesAreBoundedAndScaleDeterministically() {
        EmipokemonConfig.BalanceSettings settings = new EmipokemonConfig.BalanceSettings();
        settings.activeRewardSeconds = 1;
        settings.activeRewardCoins = -5;
        settings.questCoinMultiplier = Double.NaN;
        settings.normalize();
        assertEquals(60, settings.activeRewardSeconds);
        assertEquals(0L, settings.activeRewardCoins);
        assertEquals(1.0D, settings.questCoinMultiplier);
        assertEquals(15L, settings.scaled(10L, 1.5D));
        assertEquals(0L, settings.scaled(10L, 0.0D));
    }
}
