package com.emipokemon.config;

public final class EmipokemonConfig {
    public int configVersion = 6;
    public boolean debugLogging = false;
    public boolean playerDataEnabled = true;
    public HubSettings hub = new HubSettings();
    public BalanceSettings balance = new BalanceSettings();
    public CasinoSettings casino = new CasinoSettings();
    public DailyRewardSettings dailyRewards = new DailyRewardSettings();
    public BattlePassSettings battlePass = new BattlePassSettings();

    public void normalize() {
        if (hub == null) {
            hub = new HubSettings();
        }
        hub.normalize();
        if (balance == null) balance = new BalanceSettings();
        balance.normalize();
        if (casino == null) casino = new CasinoSettings();
        casino.normalize();
        if (dailyRewards == null) dailyRewards = new DailyRewardSettings();
        dailyRewards.normalize();
        if (battlePass == null) battlePass = new BattlePassSettings();
        battlePass.normalize();
        configVersion = 6;
    }

    public static final class DailyRewardSettings {
        public boolean enabled = true;
        /** IANA time zone used to decide when a new real day starts. */
        public String timeZone = "America/Lima";
        public boolean openOnLogin = true;
        public java.util.List<DailyRewardEntry> rewards = defaultRewards();

        public void normalize() {
            if (timeZone == null || timeZone.isBlank()) timeZone = "America/Lima";
            try { java.time.ZoneId.of(timeZone); }
            catch (Exception ignored) { timeZone = "America/Lima"; }
            if (rewards == null) rewards = defaultRewards();
            java.util.ArrayList<DailyRewardEntry> safe = new java.util.ArrayList<>();
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            for (DailyRewardEntry reward : rewards) {
                if (reward == null) continue;
                reward.normalize();
                if (reward.weight > 0 && ids.add(reward.id)) safe.add(reward);
            }
            rewards = safe.isEmpty() ? defaultRewards() : safe;
        }

        private static java.util.List<DailyRewardEntry> defaultRewards() {
            return new java.util.ArrayList<>(java.util.List.of(
                    new DailyRewardEntry("michicoins_250", "MICHICOINS", "", 250, 28),
                    new DailyRewardEntry("diamonds_3", "ITEM", "minecraft:diamond", 3, 15),
                    new DailyRewardEntry("ultra_balls_4", "ITEM", "cobblemon:ultra_ball", 4, 18),
                    new DailyRewardEntry("rare_candy_2", "ITEM", "cobblemon:rare_candy", 2, 12),
                    new DailyRewardEntry("standard_roll", "STANDARD_ROLLS", "", 1, 12),
                    new DailyRewardEntry("claw_ticket", "ITEM", "emipokemon:claw_ticket", 1, 8),
                    new DailyRewardEntry("emi_roll", "EMI_ROLLS", "", 1, 5),
                    new DailyRewardEntry("random_pokemon", "POKEMON", "standard", 1, 2)
            ));
        }
    }

    public static final class DailyRewardEntry {
        public String id;
        public String type;
        public String value;
        public int amount;
        public int weight;

        public DailyRewardEntry() { }

        public DailyRewardEntry(String id, String type, String value, int amount, int weight) {
            this.id = id;
            this.type = type;
            this.value = value;
            this.amount = amount;
            this.weight = weight;
        }

        public void normalize() {
            if (id == null || id.isBlank()) id = "reward";
            id = id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            if (type == null) type = "ITEM";
            type = type.toUpperCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("ITEM", "MICHICOINS", "STANDARD_ROLLS", "EMI_ROLLS", "POKEMON").contains(type))
                type = "ITEM";
            if (value == null) value = "";
            amount = Math.clamp(amount, 1, 1_000_000);
            weight = Math.clamp(weight, 0, 1_000_000);
        }
    }

    public static final class BattlePassSettings {
        public boolean enabled = true;
        public int baseXpPerLevel = 500;
        public int xpGrowthPerLevel = 75;
        public int maximumXpPerLevel = 5_000;
        public int activeRewardSeconds = 900;
        public int activeRewardXp = 20;
        public int questMinimumXp = 80;
        public int questMaximumXp = 400;
        public int commonCaptureXp = 8;
        public int uncommonCaptureXp = 12;
        public int rareCaptureXp = 25;
        public int epicCaptureXp = 45;
        public int legendaryCaptureXp = 120;
        public int mythicalCaptureXp = 180;
        public int newSpeciesBonusXp = 10;
        public int captureXpEventsPerMinute = 12;
        public int evolutionXp = 20;
        public int wildBattleXp = 12;
        public int trainerBattleXp = 30;
        public int newBiomeXp = 15;
        public int jobLevelXp = 40;
        public int freeRewardEveryLevels = 4;
        public int freeEmiRolls = 1;
        public int premiumFirstLevelEmiRolls = 10;
        public int premiumRewardEveryLevels = 4;
        public int premiumEmiRolls = 2;
        public String premiumPermission = "emipokemon.battlepass.premium";
        public java.util.Set<String> premiumGroups = new java.util.LinkedHashSet<>(
                java.util.List.of("vip", "donador", "mod", "moderador", "emi", "admin", "cachalota"));
        public java.util.Set<String> premiumPlayerNames = new java.util.LinkedHashSet<>(java.util.List.of("emi"));

        public void normalize() {
            baseXpPerLevel = Math.clamp(baseXpPerLevel, 50, 1_000_000);
            xpGrowthPerLevel = Math.clamp(xpGrowthPerLevel, 0, 100_000);
            maximumXpPerLevel = Math.clamp(maximumXpPerLevel, baseXpPerLevel, 10_000_000);
            activeRewardSeconds = Math.clamp(activeRewardSeconds, 60, 86_400);
            activeRewardXp = bounded(activeRewardXp);
            questMinimumXp = bounded(questMinimumXp);
            questMaximumXp = Math.clamp(questMaximumXp, questMinimumXp, 1_000_000);
            commonCaptureXp = bounded(commonCaptureXp);
            uncommonCaptureXp = bounded(uncommonCaptureXp);
            rareCaptureXp = bounded(rareCaptureXp);
            epicCaptureXp = bounded(epicCaptureXp);
            legendaryCaptureXp = bounded(legendaryCaptureXp);
            mythicalCaptureXp = bounded(mythicalCaptureXp);
            newSpeciesBonusXp = bounded(newSpeciesBonusXp);
            captureXpEventsPerMinute = Math.clamp(captureXpEventsPerMinute, 1, 10_000);
            evolutionXp = bounded(evolutionXp);
            wildBattleXp = bounded(wildBattleXp);
            trainerBattleXp = bounded(trainerBattleXp);
            newBiomeXp = bounded(newBiomeXp);
            jobLevelXp = bounded(jobLevelXp);
            freeRewardEveryLevels = Math.clamp(freeRewardEveryLevels, 1, 1_000);
            freeEmiRolls = Math.clamp(freeEmiRolls, 1, 1_000);
            premiumFirstLevelEmiRolls = Math.clamp(premiumFirstLevelEmiRolls, 1, 10_000);
            premiumRewardEveryLevels = Math.clamp(premiumRewardEveryLevels, 1, 1_000);
            premiumEmiRolls = Math.clamp(premiumEmiRolls, 1, 10_000);
            if (premiumPermission == null) premiumPermission = "emipokemon.battlepass.premium";
            if (premiumGroups == null) premiumGroups = new java.util.LinkedHashSet<>();
            if (premiumPlayerNames == null) premiumPlayerNames = new java.util.LinkedHashSet<>();
            premiumGroups = normalizeKeys(premiumGroups);
            premiumPlayerNames = normalizeKeys(premiumPlayerNames);
        }

        private static int bounded(int value) { return Math.clamp(value, 0, 1_000_000); }

        private static java.util.Set<String> normalizeKeys(java.util.Set<String> values) {
            java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
            for (String value : values) if (value != null && !value.isBlank())
                result.add(value.strip().toLowerCase(java.util.Locale.ROOT));
            return result;
        }
    }

    public static final class CasinoSettings {
        public boolean enabled = true;
        public long minimumBet = 10L;
        public long maximumBet = 100_000L;
        public long maximumPayout = 5_000_000L;
        public long chipPrice = 100L;
        public long normalTicketPrice = 1_000L;
        public long clawTicketPrice = 250L;
        public java.util.List<String> clawPlushieIds = new java.util.ArrayList<>(java.util.List.of(
                "pokeblocks:pokedoll_eevee", "pokeblocks:pokedoll_mimikyu",
                "pokeblocks:pokedoll_riolu", "pokeblocks:pokedoll_rowlet",
                "pokeblocks:pokedoll_gengar", "pokeblocks:pokedoll_snorlax",
                "pokeblocks:pokedoll_furret", "pokeblocks:pokedoll_bulbasaur",
                "pokeblocks:pokedoll_charmander", "pokeblocks:pokedoll_squirtle"));
        public double slotPayoutMultiplier = 1.0D;
        public double roulettePayoutMultiplier = 1.0D;
        public double dicePayoutMultiplier = 1.0D;
        public double blackjackPayoutMultiplier = 1.0D;
        public double pokerPayoutMultiplier = 1.0D;

        public void normalize() {
            minimumBet = Math.clamp(minimumBet, 1L, 1_000_000L);
            maximumBet = Math.clamp(maximumBet, minimumBet, 100_000_000L);
            maximumPayout = Math.clamp(maximumPayout, maximumBet, 1_000_000_000L);
            chipPrice = Math.clamp(chipPrice, 1L, 1_000_000L);
            normalTicketPrice = Math.clamp(normalTicketPrice, 1L, 100_000_000L);
            clawTicketPrice = Math.clamp(clawTicketPrice, 1L, 100_000_000L);
            if (clawPlushieIds == null) clawPlushieIds = new java.util.ArrayList<>();
            clawPlushieIds = clawPlushieIds.stream().filter(java.util.Objects::nonNull)
                    .map(String::strip).filter(id -> id.matches("pokeblocks:pokedoll_[a-z0-9_./-]+"))
                    .distinct().limit(256)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            slotPayoutMultiplier = payoutMultiplier(slotPayoutMultiplier);
            roulettePayoutMultiplier = payoutMultiplier(roulettePayoutMultiplier);
            dicePayoutMultiplier = payoutMultiplier(dicePayoutMultiplier);
            blackjackPayoutMultiplier = payoutMultiplier(blackjackPayoutMultiplier);
            pokerPayoutMultiplier = payoutMultiplier(pokerPayoutMultiplier);
        }

        private static double payoutMultiplier(double value) {
            if (!Double.isFinite(value)) return 1.0D;
            return Math.clamp(value, 0.0D, 10.0D);
        }
    }

    public static final class BalanceSettings {
        public int activeRewardSeconds = 600;
        public long activeRewardCoins = 5L;
        public double directCoinMultiplier = 1.0D;
        public double jobCoinMultiplier = 1.0D;
        public double jobXpMultiplier = 1.0D;
        public double questCoinMultiplier = 1.0D;
        public double shopPriceMultiplier = 1.0D;

        public void normalize() {
            activeRewardSeconds = Math.clamp(activeRewardSeconds, 60, 86_400);
            activeRewardCoins = Math.clamp(activeRewardCoins, 0L, 1_000_000L);
            directCoinMultiplier = clampMultiplier(directCoinMultiplier);
            jobCoinMultiplier = clampMultiplier(jobCoinMultiplier);
            jobXpMultiplier = clampMultiplier(jobXpMultiplier);
            questCoinMultiplier = clampMultiplier(questCoinMultiplier);
            shopPriceMultiplier = clampMultiplier(shopPriceMultiplier);
        }

        private static double clampMultiplier(double value) {
            if (!Double.isFinite(value)) return 1.0D;
            return Math.clamp(value, 0.0D, 100.0D);
        }

        public long scaled(long base, double multiplier) {
            if (base <= 0L || multiplier <= 0.0D) return 0L;
            double scaled = base * multiplier;
            return scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, Math.round(scaled));
        }
    }

    public static final class HubSettings {
        public boolean enabled = false;
        public String world = "minecraft:overworld";
        public double x = 0.5;
        public double y = 64.0;
        public double z = 0.5;
        public float yaw = 0.0f;
        public float pitch = 0.0f;
        public RegionCorner pos1;
        public RegionCorner pos2;

        public void normalize() {
            if (world == null || world.isBlank()) {
                world = "minecraft:overworld";
            }
            if (!Double.isFinite(x)) x = 0.5;
            if (!Double.isFinite(y)) y = 64.0;
            if (!Double.isFinite(z)) z = 0.5;
            if (!Float.isFinite(yaw)) yaw = 0.0f;
            if (!Float.isFinite(pitch)) pitch = 0.0f;
        }

        public boolean hasRegion() {
            return pos1 != null && pos2 != null;
        }

        public boolean contains(String worldId, int blockX, int blockY, int blockZ) {
            if (!enabled || !hasRegion() || !world.equals(worldId)) {
                return false;
            }
            return blockX >= Math.min(pos1.x, pos2.x) && blockX <= Math.max(pos1.x, pos2.x)
                    && blockY >= Math.min(pos1.y, pos2.y) && blockY <= Math.max(pos1.y, pos2.y)
                    && blockZ >= Math.min(pos1.z, pos2.z) && blockZ <= Math.max(pos1.z, pos2.z);
        }
    }

    public static final class RegionCorner {
        public int x;
        public int y;
        public int z;

        public RegionCorner() {
        }

        public RegionCorner(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
