package com.emipokemon.gacha.banner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannerScheduleTest {
    @Test
    void scheduleUsesInclusiveStartAndExclusiveEnd() {
        BannerDefinition banner = new BannerDefinition();
        banner.startsAtEpochMillis = 1_000L;
        banner.endsAtEpochMillis = 2_000L;
        banner.normalize();
        assertFalse(banner.activeAt(999L));
        assertTrue(banner.activeAt(1_000L));
        assertTrue(banner.activeAt(1_999L));
        assertFalse(banner.activeAt(2_000L));
        banner.enabled = false;
        assertFalse(banner.activeAt(1_500L));
    }
}
