package com.emipokemon.config;

public final class EmipokemonConfig {
    public int configVersion = 5;
    public boolean debugLogging = false;
    public boolean playerDataEnabled = true;
    public HubSettings hub = new HubSettings();
    public BalanceSettings balance = new BalanceSettings();
    public CasinoSettings casino = new CasinoSettings();

    public void normalize() {
        if (hub == null) {
            hub = new HubSettings();
        }
        hub.normalize();
        if (balance == null) balance = new BalanceSettings();
        balance.normalize();
        if (casino == null) casino = new CasinoSettings();
        casino.normalize();
        configVersion = 5;
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
