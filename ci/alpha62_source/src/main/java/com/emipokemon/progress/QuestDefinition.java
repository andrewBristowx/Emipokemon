package com.emipokemon.progress;

import java.util.List;

public record QuestDefinition(
        String id,
        String track,
        String chapter,
        String chapterTitle,
        String title,
        String description,
        String objectiveType,
        long target,
        long michicoins,
        List<RewardItem> items
) {
    public static final String PROGRESSION = "progression";
    public static final String ADVENTURE = "adventure";

    public record RewardItem(String itemId, int count) {
    }
}
