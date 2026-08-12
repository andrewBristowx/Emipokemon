package com.emipokemon.progress;

import java.util.List;

public final class QuestCatalog {
    private static final List<QuestDefinition> QUESTS = List.of(
            quest("starter", "1", "Tu aventura comienza", "Tu primera compañera",
                    "Elige a tu Pokémon inicial para comenzar tu aventura.", "starter", 1, 100,
                    item("cobblemon:poke_ball", 5)),
            quest("heal_team", "1", "Tu aventura comienza", "Una visita al Centro Pokémon",
                    "Cura a tu equipo por primera vez.", "heal", 1, 40,
                    item("cobblemon:potion", 3)),
            quest("craft_balls", "1", "Tu aventura comienza", "Tus primeras Poké Balls",
                    "Fabrica cinco Poké Balls en una mesa de crafteo.", "craft_pokeball", 5, 80),
            quest("first_capture", "1", "Tu aventura comienza", "Una nueva amistad",
                    "Captura tu primer Pokémon salvaje.", "capture", 1, 60),
            quest("first_victory", "1", "Tu aventura comienza", "Tu primera victoria",
                    "Gana un combate contra un Pokémon salvaje.", "wild_victory", 1, 60),
            quest("team_three", "1", "Tu aventura comienza", "Un pequeño equipo",
                    "Consigue tres Pokémon.", "owned_pokemon", 3, 80),
            quest("first_evolution", "1", "Tu aventura comienza", "Crecer juntos",
                    "Evoluciona un Pokémon por primera vez.", "evolution", 1, 120),

            quest("five_species", "2", "Camino de entrenador", "Conoce nuevas especies",
                    "Registra cinco especies diferentes.", "species", 5, 120),
            quest("level_fifteen", "2", "Camino de entrenador", "Cada vez más fuertes",
                    "Ten un Pokémon de nivel 15 o superior.", "pokemon_level", 15, 120),
            quest("three_wins", "2", "Camino de entrenador", "Racha de victorias",
                    "Gana tres combates Pokémon consecutivos.", "win_streak", 3, 150),
            quest("full_team", "2", "Camino de entrenador", "Equipo completo",
                    "Consigue un equipo de seis Pokémon.", "owned_pokemon", 6, 180),
            quest("explore_five", "2", "Camino de entrenador", "Conoce el mundo",
                    "Descubre cinco biomas diferentes.", "biomes", 5, 140),
            quest("job_level_three", "2", "Camino de entrenador", "Encuentra tu vocación",
                    "Alcanza nivel 3 en cualquier trabajo.", "job_level", 3, 180),

            leader("brock", "Brock", 250),
            leader("misty", "Misty", 275),
            leader("lt_surge", "Lt. Surge", 300),
            leader("erika", "Erika", 325),
            leader("koga", "Koga", 350),
            leader("sabrina", "Sabrina", 375),
            leader("blaine", "Blaine", 400),
            leader("giovanni", "Giovanni", 500),

            quest("rocket_base", "4", "Equipo Rocket", "La torre del Equipo Rocket",
                    "Entra en la base oficial del Equipo Rocket.",
                    "structure:cobbleverse:team_rocket_tower", 1, 500),
            quest("rocket_giovanni", "4", "Equipo Rocket", "Cortar la cabeza de la serpiente",
                    "Derrota al Giovanni del Equipo Rocket.",
                    "advancement:emipokemon:integration/defeat_team_rocket_giovanni", 1, 750,
                    item("emipokemon:gacha_ticket", 1)),

            quest("kanto_league_arrival", "5", "Alto Mando", "Las puertas de la Liga",
                    "Entra en la Liga Pokémon de Kanto.",
                    "structure:cobbleverse:kanto_league", 1, 350),
            elite("lorelei", "Lorelei", 600),
            elite("bruno", "Bruno", 650),
            elite("agatha", "Agatha", 700),
            elite("lance", "Lance", 750),
            quest("champion_blue", "5", "Alto Mando", "Campeón de Kanto",
                    "Derrota a Blue después de superar al Alto Mando.",
                    "advancement:cobbleverse:trainer/kanto/defeat_champion_blue", 1, 1_000,
                    item("emipokemon:gacha_ticket", 1)),

            structure("bell_tower", "Torre Campana", 400),
            structure("sky_pillar", "Pilar Celeste", 450),
            structure("spear_pillar", "Columna Lanza", 500),

            altar("articuno", "Articuno", 500),
            captureLegendary("articuno", "Articuno", 900),
            altar("zapdos", "Zapdos", 500),
            captureLegendary("zapdos", "Zapdos", 900),
            altar("moltres", "Moltres", 500),
            adventureQuest("capture_altar_moltres", "7", "Altares legendarios", "Captura a Moltres",
                    "Después de invocarlo, captura un Moltres.",
                    "capture_species:cobblemon:moltres", 1, 900,
                    item("emipokemon:gacha_ticket", 1))
    );

    private QuestCatalog() {
    }

    public static List<QuestDefinition> all() {
        return QUESTS;
    }

    private static QuestDefinition quest(String id, String chapter, String chapterTitle, String title,
                                         String description, String type, long target, long coins,
                                         QuestDefinition.RewardItem... items) {
        return new QuestDefinition(id, QuestDefinition.PROGRESSION, chapter, chapterTitle, title,
                description, type, target, coins, List.of(items));
    }

    private static QuestDefinition adventureQuest(String id, String chapter, String chapterTitle, String title,
                                                   String description, String type, long target, long coins,
                                                   QuestDefinition.RewardItem... items) {
        return new QuestDefinition(id, QuestDefinition.ADVENTURE, chapter, chapterTitle, title,
                description, type, target, coins, List.of(items));
    }

    private static QuestDefinition leader(String id, String name, long coins) {
        return quest("defeat_" + id, "3", "Liga de Kanto", "Desafía a " + name,
                "Derrota a " + name + " en su combate oficial.",
                "advancement:cobbleverse:trainer/kanto/defeat_" + advancementLeaderId(id), 1, coins);
    }

    private static QuestDefinition elite(String id, String name, long coins) {
        return quest("elite_" + id, "5", "Alto Mando", "Desafía a " + name,
                "Derrota a " + name + " en el combate oficial de la Liga.",
                "advancement:cobbleverse:trainer/kanto/defeat_elite_" + id, 1, coins);
    }

    private static QuestDefinition structure(String id, String name, long coins) {
        return adventureQuest("visit_" + id, "6", "Estructuras especiales", "Explora " + name,
                "Entra en la estructura oficial de " + name + ".",
                "structure:cobbleverse:" + id, 1, coins);
    }

    private static QuestDefinition altar(String species, String name, long coins) {
        return adventureQuest("invoke_" + species, "7", "Altares legendarios", "Invoca a " + name,
                "Activa correctamente el altar de " + name + ".",
                "altar:lumymon:" + species + "_altar", 1, coins);
    }

    private static QuestDefinition captureLegendary(String species, String name, long coins) {
        return adventureQuest("capture_altar_" + species, "7", "Altares legendarios", "Captura a " + name,
                "Después de invocarlo, captura un " + name + ".",
                "capture_species:cobblemon:" + species, 1, coins);
    }

    private static QuestDefinition.RewardItem item(String id, int count) {
        return new QuestDefinition.RewardItem(id, count);
    }

    private static String advancementLeaderId(String id) {
        return "lt_surge".equals(id) ? "ltsurge" : id;
    }
}
