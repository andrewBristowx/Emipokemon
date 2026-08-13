package com.emipokemon.progress;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestCatalogTest {
    @Test
    void keepsEveryQuestIdUnique() {
        Set<String> ids = new HashSet<>();
        for (QuestDefinition quest : QuestCatalog.all()) {
            assertTrue(ids.add(quest.id()), "Duplicate quest id: " + quest.id());
        }
        assertEquals(38, ids.size());
    }

    @Test
    void usesOnlyAuditedCobbleverseObjectives() {
        Set<String> objectives = new HashSet<>();
        for (QuestDefinition quest : QuestCatalog.all()) objectives.add(quest.objectiveType());

        assertTrue(objectives.contains("structure:cobbleverse:team_rocket_tower"));
        assertTrue(objectives.contains("advancement:emipokemon:integration/defeat_team_rocket_giovanni"));
        assertTrue(objectives.contains("structure:cobbleverse:kanto_league"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_elite_lorelei"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_elite_bruno"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_elite_agatha"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_elite_lance"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_champion_blue"));
        assertTrue(objectives.contains("structure:cobbleverse:bell_tower"));
        assertTrue(objectives.contains("structure:cobbleverse:sky_pillar"));
        assertTrue(objectives.contains("structure:cobbleverse:spear_pillar"));
        assertTrue(objectives.contains("altar:lumymon:articuno_altar"));
        assertTrue(objectives.contains("altar:lumymon:zapdos_altar"));
        assertTrue(objectives.contains("altar:lumymon:moltres_altar"));
        assertTrue(objectives.contains("capture_species:cobblemon:articuno"));
        assertTrue(objectives.contains("capture_species:cobblemon:zapdos"));
        assertTrue(objectives.contains("capture_species:cobblemon:moltres"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_brock"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_misty"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_ltsurge"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_erika"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_koga"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_sabrina"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_blaine"));
        assertTrue(objectives.contains("advancement:cobbleverse:trainer/kanto/defeat_giovanni"));
        assertTrue(objectives.stream().noneMatch(value -> value.startsWith("leader:")));
    }

    @Test
    void separatesProgressionFromAdventureWithoutChangingQuestIds() {
        long progression = QuestCatalog.all().stream()
                .filter(quest -> QuestDefinition.PROGRESSION.equals(quest.track())).count();
        long adventure = QuestCatalog.all().stream()
                .filter(quest -> QuestDefinition.ADVENTURE.equals(quest.track())).count();

        assertEquals(29, progression);
        assertEquals(9, adventure);
        for (QuestDefinition quest : QuestCatalog.all()) {
            if (quest.chapter().equals("6") || quest.chapter().equals("7")) {
                assertEquals(QuestDefinition.ADVENTURE, quest.track());
            }
        }
    }

    @Test
    void specialRewardsNeverExposeProtectionEmi() {
        for (QuestDefinition quest : QuestCatalog.all()) {
            for (QuestDefinition.RewardItem reward : quest.items()) {
                assertTrue(!reward.itemId().contains("protection_emi"));
                assertTrue(!reward.itemId().contains("protection_core_emi"));
            }
        }
    }
}
