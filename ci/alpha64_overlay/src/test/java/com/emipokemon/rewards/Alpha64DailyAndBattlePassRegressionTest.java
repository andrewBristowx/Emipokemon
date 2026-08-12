package com.emipokemon.rewards;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Alpha64DailyAndBattlePassRegressionTest {
    private static String source(String path) throws Exception { return Files.readString(Path.of("src", path)); }

    @Test void dailyClaimIsPersistedBeforeExternalDeliveryAndRecovers() throws Exception {
        String daily = source("main/java/com/emipokemon/rewards/DailyRewardService.java");
        assertTrue(daily.indexOf("writeOperation(operation)") < daily.indexOf("dataManager.saveNowChecked(player.getUuid())"));
        assertTrue(daily.indexOf("dataManager.saveNowChecked(player.getUuid())") < daily.indexOf("deliverExternal(player, operation)"));
        assertTrue(daily.contains("recoverForPlayer"));
        assertTrue(daily.contains("daily-reward-audit.log"));
        assertTrue(daily.contains("ZoneId.of(settings().timeZone)"));
    }

    @Test void dailyPoolContainsEveryRequestedRewardFamily() throws Exception {
        String config = source("main/java/com/emipokemon/config/EmipokemonConfig.java");
        assertTrue(config.contains("minecraft:diamond"));
        assertTrue(config.contains("cobblemon:ultra_ball"));
        assertTrue(config.contains("cobblemon:rare_candy"));
        assertTrue(config.contains("STANDARD_ROLLS"));
        assertTrue(config.contains("EMI_ROLLS"));
        assertTrue(config.contains("MICHICOINS"));
        assertTrue(config.contains("POKEMON"));
    }

    @Test void infinitePassUsesSpecifiedFreeAndPremiumRules() throws Exception {
        String config = source("main/java/com/emipokemon/config/EmipokemonConfig.java");
        String pass = source("main/java/com/emipokemon/rewards/BattlePassService.java");
        assertTrue(config.contains("freeRewardEveryLevels = 4"));
        assertTrue(config.contains("freeEmiRolls = 1"));
        assertTrue(config.contains("premiumFirstLevelEmiRolls = 10"));
        assertTrue(config.contains("premiumRewardEveryLevels = 4"));
        assertTrue(config.contains("premiumEmiRolls = 2"));
        assertTrue(pass.contains("level == 1"));
        assertTrue(pass.contains("totalXpForLevel"));
        assertTrue(pass.contains("claimedPremium"));
    }

    @Test void passXpIsFedByAllRequestedActivitiesAndRateLimited() throws Exception {
        String progression = source("main/java/com/emipokemon/progress/ProgressionService.java");
        String pass = source("main/java/com/emipokemon/rewards/BattlePassService.java");
        assertTrue(progression.contains("onActiveSecond"));
        assertTrue(progression.contains("onQuestClaim"));
        assertTrue(progression.contains("onCapture"));
        assertTrue(progression.contains("onJobLevel"));
        assertTrue(progression.contains("onBattleVictory"));
        assertTrue(pass.contains("captureXpEventsPerMinute"));
    }

    @Test void virtualGachaCreditsAreAtomicAndConsumedBeforePhysicalTickets() throws Exception {
        String wallet = source("main/java/com/emipokemon/rewards/RewardWalletService.java");
        String currency = source("main/java/com/emipokemon/gacha/economy/GachaCurrencyService.java");
        String machine = source("main/java/com/emipokemon/gacha/machine/GachaMachineBlockEntity.java");
        assertTrue(wallet.contains("saveNowChecked"));
        assertTrue(wallet.contains("VIRTUAL_EMI"));
        assertTrue(currency.contains("wallet.withdraw"));
        assertTrue(currency.contains("wallet.refund"));
        assertTrue(machine.contains("rewardWalletService().balance"));
    }

    @Test void cobblemonPortraitAdapterSupportsActual173Signature() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/PokemonPortraitRenderer.java");
        String casino = source("client/java/com/emipokemon/client/casino/CasinoScreen.java");
        assertTrue(renderer.contains("getParameterCount() == 13"));
        assertTrue(renderer.contains("getParameterCount() == 16"));
        assertTrue(casino.contains("PokemonPortraitRenderer.draw"));
        assertTrue(casino.contains("POKÉMON EN CUSTODIA SEGURA"));
        assertTrue(casino.contains("!\"claw\".equals(game) && !\"pokemon_flip\".equals(game)"));
    }

    @Test void generatedDailyAndPassAssetsHaveExactImplementableCanvas() throws Exception {
        var daily = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/gui/daily_reward.png").toFile());
        var pass = ImageIO.read(Path.of("src/main/resources/assets/emipokemon/textures/gui/battle_pass.png").toFile());
        assertEquals(1536, daily.getWidth());
        assertEquals(1024, daily.getHeight());
        assertEquals(1536, pass.getWidth());
        assertEquals(1024, pass.getHeight());
    }
}
