package com.emipokemon.progress;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestRewardSafetyTest {
    @Test
    void reservesAndPersistsTheClaimBeforeGrantingRewards() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/emipokemon/progress/ProgressionService.java"));
        int methodStart = source.indexOf("synchronized boolean claimCurrentQuest");
        int methodEnd = source.indexOf("public boolean signal", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("data.quests.claimed.contains(quest.id())"));
        int reserve = method.indexOf("data.quests.claimed.add(quest.id())");
        int persist = method.indexOf("dataManager.saveNow(player.getUuid())", reserve);
        int scaled = method.indexOf("long questCoins = settings().scaled", persist);
        int credit = method.indexOf("credit(player, questCoins", scaled);
        int items = method.indexOf("giveItem(player, reward)", credit);
        assertTrue(reserve >= 0 && reserve < persist && persist < scaled && scaled < credit && credit < items);
    }
}
