package com.emipokemon.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.gacha.GachaRollResult;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import com.emipokemon.gacha.reward.CobblemonRewardService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One authoritative reward per real calendar day in the configured server time zone. */
public final class DailyRewardService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PlayerDataManager dataManager;
    private final ConfigManager configManager;
    private final CobblemonRewardService pokemonRewards = new CobblemonRewardService();
    private final Map<UUID, Integer> delayedOpen = new HashMap<>();
    private final Map<UUID, DailyRewardSnapshot.RewardView> lastReveal = new HashMap<>();
    private final Path operationDirectory;
    private final Path auditFile;

    public DailyRewardService(PlayerDataManager dataManager, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.operationDirectory = configManager.configDirectory().resolve("daily-reward-operations");
        this.auditFile = configManager.configDirectory().resolve("daily-reward-audit.log");
    }

    public void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public synchronized void playerJoined(ServerPlayerEntity player) {
        recoverForPlayer(player);
        if (settings().enabled && settings().openOnLogin && eligible(player.getUuid())) delayedOpen.put(player.getUuid(), 40);
    }

    public synchronized void playerLeft(UUID playerId) {
        delayedOpen.remove(playerId);
        lastReveal.remove(playerId);
    }

    public synchronized ClaimResult claim(ServerPlayerEntity player) {
        if (player == null || !settings().enabled) return new ClaimResult(false, "Las recompensas diarias están desactivadas.");
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        LocalDate today = today();
        if (today.toString().equals(data.dailyReward.lastClaimDate))
            return new ClaimResult(false, "Ya reclamaste la recompensa de hoy.");

        Operation operation = prepareOperation(player, today);
        if (operation == null) return new ClaimResult(false, "No hay premios diarios válidos disponibles.");
        try { writeOperation(operation); }
        catch (Exception exception) {
            return new ClaimResult(false, "No se pudo reservar la recompensa; no se marcó el día como reclamado.");
        }

        String oldDate = data.dailyReward.lastClaimDate;
        int oldStreak = data.dailyReward.streak;
        int oldClaims = data.dailyReward.totalClaims;
        String oldLabel = data.dailyReward.lastRewardLabel;
        long oldCoins = data.economy.michicoins;
        long oldEarned = data.economy.lifetimeEarned;
        long oldStandard = data.rewardWallet.standardRolls;
        long oldEmi = data.rewardWallet.emiRolls;

        LocalDate previous = parseDate(oldDate);
        data.dailyReward.lastClaimDate = today.toString();
        data.dailyReward.streak = today.minusDays(1).equals(previous) ? oldStreak + 1 : 1;
        data.dailyReward.totalClaims = oldClaims + 1;
        data.dailyReward.lastRewardLabel = operation.label;
        applyPersistentReward(data, operation);
        if (!dataManager.saveNowChecked(player.getUuid())) {
            data.dailyReward.lastClaimDate = oldDate;
            data.dailyReward.streak = oldStreak;
            data.dailyReward.totalClaims = oldClaims;
            data.dailyReward.lastRewardLabel = oldLabel;
            data.economy.michicoins = oldCoins;
            data.economy.lifetimeEarned = oldEarned;
            data.rewardWallet.standardRolls = oldStandard;
            data.rewardWallet.emiRolls = oldEmi;
            deleteOperation(operation);
            return new ClaimResult(false, "No se pudo guardar el reclamo; no se consumió la recompensa diaria.");
        }

        boolean delivered = deliverExternal(player, operation);
        if (!delivered) {
            operation.status = "PREPARED";
            writeOperation(operation);
            audit(player.getUuid(), "delivery_pending", operation.id + ":" + operation.label);
            return new ClaimResult(true, "Premio reservado. Se volverá a entregar automáticamente al reconectar.");
        }
        operation.status = "DELIVERED";
        writeOperation(operation);
        DailyRewardSnapshot.RewardView reveal = view(operation);
        lastReveal.put(player.getUuid(), reveal);
        audit(player.getUuid(), "delivered", operation.id + ":" + operation.label);
        player.sendMessage(Text.literal("§d✦ Recompensa diaria: §f" + operation.label), false);
        return new ClaimResult(true, "¡Recompensa diaria obtenida!");
    }

    public synchronized DailyRewardSnapshot snapshot(ServerPlayerEntity player, String message) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        DailyRewardSnapshot snapshot = new DailyRewardSnapshot();
        snapshot.eligible = eligible(player.getUuid());
        snapshot.nextClaimEpochMillis = nextClaimEpochMillis();
        snapshot.streak = data.dailyReward.streak;
        snapshot.totalClaims = data.dailyReward.totalClaims;
        snapshot.lastReward = data.dailyReward.lastRewardLabel;
        snapshot.message = message == null ? "" : message;
        snapshot.revealed = lastReveal.get(player.getUuid());
        for (EmipokemonConfig.DailyRewardEntry entry : settings().rewards) snapshot.possibleRewards.add(view(entry));
        return snapshot;
    }

    private Operation prepareOperation(ServerPlayerEntity player, LocalDate date) {
        EmipokemonConfig.DailyRewardEntry selected = weightedReward();
        if (selected == null) return null;
        Operation operation = new Operation();
        operation.id = UUID.randomUUID();
        operation.player = player.getUuid();
        operation.claimDate = date.toString();
        operation.type = selected.type;
        operation.value = selected.value;
        operation.amount = selected.amount;
        operation.status = "PREPARED";
        operation.createdAt = System.currentTimeMillis();
        if ("POKEMON".equals(selected.type)) {
            GachaRollResult roll = Emipokemon.gachaService().simulate(selected.value.isBlank() ? "standard" : selected.value);
            if (roll == null) return null;
            operation.speciesId = roll.pokemon().speciesId();
            operation.speciesName = roll.pokemon().displayName();
            operation.level = roll.level();
            operation.shiny = roll.shiny();
            operation.tier = roll.tier().name();
            operation.label = operation.speciesName + " Nv." + operation.level + (operation.shiny ? " shiny" : "");
        } else if ("ITEM".equals(selected.type)) {
            Identifier id = Identifier.tryParse(selected.value);
            if (id == null || !Registries.ITEM.containsId(id) || Registries.ITEM.get(id) == Items.AIR) return null;
            Item item = Registries.ITEM.get(id);
            operation.label = selected.amount + "× " + item.getName().getString();
        } else if ("MICHICOINS".equals(selected.type)) operation.label = selected.amount + " Michicoins";
        else if ("EMI_ROLLS".equals(selected.type)) operation.label = selected.amount + " tirada" + plural(selected.amount) + " de Emi";
        else if ("STANDARD_ROLLS".equals(selected.type)) operation.label = selected.amount + " tirada" + plural(selected.amount) + " estándar";
        else return null;
        return operation;
    }

    private void applyPersistentReward(PlayerData data, Operation operation) {
        if ("MICHICOINS".equals(operation.type)) {
            long amount = operation.amount;
            data.economy.michicoins = safeAdd(data.economy.michicoins, amount);
            data.economy.lifetimeEarned = safeAdd(data.economy.lifetimeEarned, amount);
        } else if ("EMI_ROLLS".equals(operation.type)) {
            data.rewardWallet.emiRolls = safeAdd(data.rewardWallet.emiRolls, operation.amount);
        } else if ("STANDARD_ROLLS".equals(operation.type)) {
            data.rewardWallet.standardRolls = safeAdd(data.rewardWallet.standardRolls, operation.amount);
        }
    }

    private boolean deliverExternal(ServerPlayerEntity player, Operation operation) {
        if ("ITEM".equals(operation.type)) {
            Identifier id = Identifier.tryParse(operation.value);
            if (id == null || !Registries.ITEM.containsId(id)) return false;
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) return false;
            int remaining = operation.amount;
            while (remaining > 0) {
                int count = Math.min(item.getMaxCount(), remaining);
                ItemStack stack = new ItemStack(item, count);
                player.getInventory().insertStack(stack);
                if (!stack.isEmpty()) player.dropItem(stack, false);
                remaining -= count;
            }
            player.getInventory().markDirty();
            return true;
        }
        if ("POKEMON".equals(operation.type)) {
            PokemonCatalogEntry entry = Emipokemon.pokemonCatalog().get(operation.speciesId);
            if (entry == null) return false;
            GachaRollResult result = new GachaRollResult("daily", entry,
                    GachaTier.parse(operation.tier, entry.tier()), operation.level, operation.shiny, false, false);
            return pokemonRewards.deliver(player, result);
        }
        return true;
    }

    private void recoverForPlayer(ServerPlayerEntity player) {
        try {
            Files.createDirectories(operationDirectory);
            try (var paths = Files.list(operationDirectory)) {
                for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".json")).toList()) {
                    Operation operation = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Operation.class);
                    if (operation == null || !player.getUuid().equals(operation.player) || !"PREPARED".equals(operation.status)) continue;
                    PlayerData data = dataManager.getOrLoad(player.getUuid());
                    if (!java.util.Objects.equals(operation.claimDate, data.dailyReward.lastClaimDate)) {
                        deleteOperation(operation);
                        continue;
                    }
                    if (deliverExternal(player, operation)) {
                        operation.status = "DELIVERED";
                        writeOperation(operation);
                        lastReveal.put(player.getUuid(), view(operation));
                        audit(player.getUuid(), "recovered", operation.id + ":" + operation.label);
                    }
                }
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not recover daily reward for {}", player.getUuid(), exception);
        }
    }

    private void tick(MinecraftServer server) {
        synchronized (this) {
            var iterator = delayedOpen.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                int remaining = entry.getValue() - 1;
                if (remaining > 0) { entry.setValue(remaining); continue; }
                iterator.remove();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null && eligible(player.getUuid())) DailyRewardNetworking.open(player, "Tu recompensa diaria está lista.");
            }
        }
    }

    private EmipokemonConfig.DailyRewardEntry weightedReward() {
        List<EmipokemonConfig.DailyRewardEntry> values = new ArrayList<>(settings().rewards);
        long total = values.stream().mapToLong(value -> Math.max(0, value.weight)).sum();
        if (total <= 0L) return null;
        long roll = RANDOM.nextLong(total);
        long cursor = 0L;
        for (EmipokemonConfig.DailyRewardEntry value : values) {
            cursor += Math.max(0, value.weight);
            if (roll < cursor) return value;
        }
        return values.get(values.size() - 1);
    }

    private DailyRewardSnapshot.RewardView view(EmipokemonConfig.DailyRewardEntry entry) {
        DailyRewardSnapshot.RewardView view = new DailyRewardSnapshot.RewardView();
        view.type = entry.type;
        view.value = entry.value;
        view.amount = entry.amount;
        view.weight = entry.weight;
        view.label = switch (entry.type) {
            case "MICHICOINS" -> entry.amount + " Michicoins";
            case "EMI_ROLLS" -> entry.amount + " tirada" + plural(entry.amount) + " de Emi";
            case "STANDARD_ROLLS" -> entry.amount + " tirada" + plural(entry.amount) + " estándar";
            case "POKEMON" -> "Pokémon aleatorio";
            default -> entry.amount + "× " + entry.value;
        };
        return view;
    }

    private DailyRewardSnapshot.RewardView view(Operation operation) {
        DailyRewardSnapshot.RewardView view = new DailyRewardSnapshot.RewardView();
        view.type = operation.type;
        view.value = operation.value;
        view.amount = operation.amount;
        view.label = operation.label;
        view.speciesId = operation.speciesId == null ? "" : "cobblemon:" + operation.speciesId.replace("cobblemon:", "");
        view.level = operation.level;
        view.shiny = operation.shiny;
        return view;
    }

    private boolean eligible(UUID playerId) {
        return settings().enabled && !today().toString().equals(dataManager.getOrLoad(playerId).dailyReward.lastClaimDate);
    }

    private LocalDate today() { return LocalDate.now(ZoneId.of(settings().timeZone)); }
    private LocalDate parseDate(String value) { try { return LocalDate.parse(value); } catch (Exception ignored) { return null; } }
    private long nextClaimEpochMillis() {
        ZoneId zone = ZoneId.of(settings().timeZone);
        return today().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
    }
    private EmipokemonConfig.DailyRewardSettings settings() { return configManager.get().dailyRewards; }
    private long safeAdd(long left, long right) { return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right; }
    private static String plural(int amount) { return amount == 1 ? "" : "s"; }

    private void writeOperation(Operation operation) {
        try {
            Files.createDirectories(operationDirectory);
            Path target = operationDirectory.resolve(operation.id + ".json");
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(operation), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist daily reward operation", exception);
        }
    }

    private void deleteOperation(Operation operation) {
        try { Files.deleteIfExists(operationDirectory.resolve(operation.id + ".json")); }
        catch (Exception exception) { Emipokemon.LOGGER.error("Could not remove aborted daily reward operation", exception); }
    }

    private void audit(UUID playerId, String action, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            Files.writeString(auditFile, System.currentTimeMillis() + "\t" + playerId + "\t" + action + "\t" + detail
                            + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) { Emipokemon.LOGGER.error("Could not write daily reward audit", exception); }
    }

    public record ClaimResult(boolean success, String message) { }

    private static final class Operation {
        UUID id;
        UUID player;
        String claimDate;
        String type;
        String value;
        int amount;
        String label;
        String speciesId;
        String speciesName;
        int level;
        boolean shiny;
        String tier;
        String status;
        long createdAt;
    }
}
