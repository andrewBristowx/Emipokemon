package com.emipokemon.progress;

import java.util.Arrays;

public enum JobType {
    TRAINER("trainer", "Entrenador", "Combates contra Pokémon, entrenadores y líderes"),
    CAPTURER("capturer", "Capturador", "Capturas y especies nuevas"),
    EXPLORER("explorer", "Explorador", "Biomas y lugares nuevos"),
    MINER("miner", "Minero", "Minerales obtenidos legítimamente"),
    FARMER("farmer", "Granjero", "Cultivos maduros recolectados"),
    BUILDER("builder", "Constructor", "Bloques colocados al construir"),
    CARETAKER("caretaker", "Cuidador", "Curaciones y evoluciones Pokémon");

    private final String id;
    private final String displayName;
    private final String description;

    JobType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }

    public static JobType byId(String id) {
        if (id == null) return null;
        return Arrays.stream(values()).filter(job -> job.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public static int levelFor(long xp) {
        int level = 1;
        long needed = 100L;
        long remaining = Math.max(0L, xp);
        while (level < 50 && remaining >= needed) {
            remaining -= needed;
            level++;
            needed = 100L + (long) (level - 1) * 50L;
        }
        return level;
    }

    public static long levelFloor(int targetLevel) {
        long total = 0L;
        for (int level = 1; level < Math.max(1, targetLevel); level++) {
            total += 100L + (long) (level - 1) * 50L;
        }
        return total;
    }

    public static long levelCeiling(int currentLevel) {
        return levelFloor(Math.min(50, currentLevel + 1));
    }
}
