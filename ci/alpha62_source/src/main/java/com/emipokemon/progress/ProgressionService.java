package com.emipokemon.progress;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.api.events.pokemon.healing.PokemonHealedEvent;
import com.cobblemon.mod.common.api.events.starter.StarterChosenEvent;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.emipokemon.Emipokemon;
import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import com.emipokemon.gacha.catalog.PokemonCatalogService;
import com.emipokemon.progress.data.QuestProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ProgressionService {
    private static final Gson GSON = new GsonBuilder().create();
    private final PlayerDataManager dataManager;
    private final PokemonCatalogService catalog;
    private final ConfigManager configManager;
    private final Map<UUID, ActivitySession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHealEvent = new ConcurrentHashMap<>();
    private final Path ledger = FabricLoader.getInstance().getConfigDir()
            .resolve(Emipokemon.MOD_ID).resolve("economy").resolve("transactions.log");
    private int ticks;

    public ProgressionService(PlayerDataManager dataManager, PokemonCatalogService catalog, ConfigManager configManager) {
        this.dataManager = dataManager;
        this.catalog = catalog;
        this.configManager = configManager;
    }

    public void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) onBlockBroken(serverPlayer, state);
        });
        CobblemonEvents.STARTER_CHOSEN.subscribe((Consumer<StarterChosenEvent>) this::onStarter);
        CobblemonEvents.POKEMON_CAPTURED.subscribe((Consumer<PokemonCapturedEvent>) this::onCapture);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe((Consumer<EvolutionCompleteEvent>) this::onEvolution);
        CobblemonEvents.POKEMON_HEALED.subscribe((Consumer<PokemonHealedEvent>) this::onHeal);
        CobblemonEvents.BATTLE_VICTORY.subscribe((Consumer<BattleVictoryEvent>) this::onBattleVictory);
        Emipokemon.LOGGER.info("Michicoins, jobs and quest progression initialized");
    }

    public void playerJoined(ServerPlayerEntity player) {
        dataManager.load(player.getUuid());
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        enforceJobLimit(player, data);
        sessions.put(player.getUuid(), ActivitySession.at(player));
        refreshAggregateObjectives(player);
    }

    public void playerLeft(UUID playerId) {
        sessions.remove(playerId);
        lastHealEvent.remove(playerId);
    }

    public long balance(UUID playerId) {
        return dataManager.getOrLoad(playerId).economy.michicoins;
    }

    public boolean adminSet(ServerPlayerEntity target, long amount, String actor) {
        PlayerData data = dataManager.getOrLoad(target.getUuid());
        long before = data.economy.michicoins;
        data.economy.michicoins = Math.max(0L, amount);
        if (data.economy.michicoins >= before) data.economy.lifetimeEarned += data.economy.michicoins - before;
        else data.economy.lifetimeSpent += before - data.economy.michicoins;
        dataManager.saveNow(target.getUuid());
        audit(target.getUuid(), data.economy.michicoins - before, data.economy.michicoins, "admin_set:" + actor);
        return true;
    }

    public long adminGive(ServerPlayerEntity target, long amount, String actor) {
        return credit(target, Math.max(0L, amount), "admin_give:" + actor);
    }

    public boolean adminTake(ServerPlayerEntity target, long amount, String actor) {
        return debit(target, Math.max(0L, amount), "admin_take:" + actor);
    }

    public boolean spend(ServerPlayerEntity player, long amount, String reason) {
        return debit(player, amount, reason);
    }

    public long refund(ServerPlayerEntity player, long amount, String reason) {
        return credit(player, amount, reason);
    }

    public synchronized long refund(UUID playerId, long requested, String reason) {
        if (playerId == null || requested <= 0L) return 0L;
        PlayerData data = dataManager.getOrLoad(playerId);
        long amount = requested;
        data.economy.michicoins = Math.addExact(data.economy.michicoins, amount);
        data.economy.lifetimeEarned = Math.addExact(data.economy.lifetimeEarned, amount);
        dataManager.saveNow(playerId);
        audit(playerId, amount, data.economy.michicoins, reason);
        return amount;
    }

    public boolean joinJob(ServerPlayerEntity player, String jobId) {
        JobType job = JobType.byId(jobId);
        if (job == null) return false;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        enforceJobLimit(player, data);
        if (data.jobs.isActive(job.id())) {
            player.sendMessage(Text.literal("§7Ya tienes activo el trabajo §f" + job.displayName() + "§7."), false);
            return true;
        }
        int limit = JobAccessPolicy.maxActiveJobs(player);
        if (data.jobs.activeJobs.size() >= limit) {
            player.sendMessage(Text.literal("§cYa utilizas tus " + limit + " espacios de trabajo. Quita uno antes de elegir otro."), false);
            return false;
        }
        data.jobs.activeJobs.add(job.id());
        dataManager.saveNow(player.getUuid());
        player.sendMessage(Text.literal("§dTrabajo añadido: §f" + job.displayName()
                + " §7(" + data.jobs.activeJobs.size() + "/" + limit + ")"), false);
        return true;
    }

    public boolean leaveJob(ServerPlayerEntity player, String jobId) {
        JobType job = JobType.byId(jobId);
        if (job == null) return false;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        if (!data.jobs.activeJobs.remove(job.id())) return false;
        dataManager.saveNow(player.getUuid());
        player.sendMessage(Text.literal("§7Trabajo retirado: §f" + job.displayName()), false);
        return true;
    }

    public void leaveAllJobs(ServerPlayerEntity player) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        data.jobs.activeJobs.clear();
        dataManager.saveNow(player.getUuid());
        player.sendMessage(Text.literal("§7Has dejado todos tus trabajos."), false);
    }

    public synchronized boolean claimCurrentQuest(ServerPlayerEntity player, String requestedTrack) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        QuestDefinition quest = activeQuest(data, normalizeTrack(requestedTrack));
        if (quest == null || !isQuestComplete(data, quest) || data.quests.claimed.contains(quest.id())) return false;
        if (!quest.items().isEmpty() && player.getInventory().getEmptySlot() < 0) {
            player.sendMessage(Text.literal("§cHaz al menos un espacio en tu inventario para reclamar."), false);
            return false;
        }

        data.quests.completed.add(quest.id());
        data.quests.claimed.add(quest.id());
        dataManager.saveNow(player.getUuid());
        audit(player.getUuid(), 0L, data.economy.michicoins, "quest_claim_reserved:" + quest.id());

        long questCoins = settings().scaled(quest.michicoins(), settings().questCoinMultiplier);
        credit(player, questCoins, "quest:" + quest.id());
        for (QuestDefinition.RewardItem reward : quest.items()) giveItem(player, reward);
        player.sendMessage(Text.literal("§d✦ Misión completada: §f" + quest.title()
                + " §7(+" + questCoins + " Michicoins)"), false);
        refreshAggregateObjectives(player);
        return true;
    }

    public boolean signal(ServerPlayerEntity player, String signal) {
        if (signal == null || signal.isBlank()) return false;
        recordObjective(player, signal.toLowerCase(Locale.ROOT), 1L, false);
        return true;
    }

    public void resetQuests(ServerPlayerEntity player) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        data.quests = new QuestProgress();
        dataManager.saveNow(player.getUuid());
        refreshAggregateObjectives(player);
        player.sendMessage(Text.literal("§eTu progreso de misiones fue reiniciado por un administrador."), false);
    }

    public String snapshotJson(ServerPlayerEntity player, String requestedTrack) {
        refreshAggregateObjectives(player);
        return GSON.toJson(snapshot(player, requestedTrack));
    }

    public JournalSnapshot snapshot(ServerPlayerEntity player, String requestedTrack) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        enforceJobLimit(player, data);
        String track = normalizeTrack(requestedTrack);
        JournalSnapshot snapshot = new JournalSnapshot();
        snapshot.questTrack = track;
        snapshot.balance = data.economy.michicoins;
        snapshot.totalAllQuests = QuestCatalog.all().size();
        snapshot.totalQuests = (int) QuestCatalog.all().stream()
                .filter(quest -> quest.track().equals(track)).count();
        snapshot.completedQuests = (int) QuestCatalog.all().stream()
                .filter(quest -> quest.track().equals(track))
                .filter(quest -> data.quests.claimed.contains(quest.id())).count();
        snapshot.claimableQuests = (int) QuestCatalog.all().stream()
                .filter(quest -> !data.quests.claimed.contains(quest.id()))
                .filter(quest -> isQuestComplete(data, quest)).count();
        snapshot.activeJobCount = data.jobs.activeJobs.size();
        snapshot.maxActiveJobs = JobAccessPolicy.maxActiveJobs(player);

        QuestDefinition active = activeQuest(data, track);
        if (active != null) {
            JournalSnapshot.QuestView view = new JournalSnapshot.QuestView();
            view.id = active.id();
            view.chapter = active.chapter();
            view.chapterTitle = active.chapterTitle();
            view.title = active.title();
            view.description = active.description();
            view.objective = objectiveLabel(active);
            view.progress = objectiveValue(data, active);
            view.target = active.target();
            view.coins = active.michicoins();
            view.complete = view.progress >= view.target;
            view.claimed = data.quests.claimed.contains(active.id());
            for (QuestDefinition.RewardItem item : active.items()) view.items.add(item.count() + "× " + item.itemId());
            snapshot.quest = view;
        }

        Map<String, JournalSnapshot.ChapterView> chapters = new LinkedHashMap<>();
        for (QuestDefinition quest : QuestCatalog.all()) {
            if (!quest.track().equals(track)) continue;
            JournalSnapshot.ChapterView chapter = chapters.computeIfAbsent(quest.chapter(), ignored -> {
                JournalSnapshot.ChapterView created = new JournalSnapshot.ChapterView();
                created.id = quest.chapter();
                created.title = quest.chapterTitle();
                return created;
            });
            chapter.total++;
            if (data.quests.claimed.contains(quest.id())) chapter.complete++;
        }
        boolean unlocked = true;
        for (JournalSnapshot.ChapterView chapter : chapters.values()) {
            chapter.unlocked = unlocked;
            unlocked = unlocked && chapter.complete >= chapter.total;
            snapshot.chapters.add(chapter);
        }

        for (JobType job : JobType.values()) {
            JournalSnapshot.JobView view = new JournalSnapshot.JobView();
            view.id = job.id();
            view.name = job.displayName();
            view.description = job.description();
            view.xp = data.jobs.xp(job.id());
            view.level = JobType.levelFor(view.xp);
            view.levelStart = JobType.levelFloor(view.level);
            view.nextLevel = view.level >= 50 ? view.xp : JobType.levelCeiling(view.level);
            view.active = data.jobs.isActive(job.id());
            snapshot.jobs.add(view);
        }
        return snapshot;
    }

    public void onCrafted(ServerPlayerEntity player, ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id.getPath().contains("poke_ball") || id.getPath().contains("pokeball")) {
            markActive(player);
            recordObjective(player, "craft_pokeball", Math.max(1, stack.getCount()), false);
        }
    }

    public void onBlockPlaced(ServerPlayerEntity player) {
        markActive(player);
        rewardJobAction(player, JobType.BUILDER, 1L, 0L, "block_place");
    }

    private void tick(MinecraftServer server) {
        if (++ticks % 20 != 0) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(ServerPlayerEntity player) {
        ActivitySession session = sessions.computeIfAbsent(player.getUuid(), ignored -> ActivitySession.at(player));
        boolean moved = session.updateMovement(player);
        if (moved) session.lastActiveAt = System.currentTimeMillis();
        boolean active = System.currentTimeMillis() - session.lastActiveAt <= 120_000L;
        if (active) {
            PlayerData data = dataManager.getOrLoad(player.getUuid());
            data.economy.activeSecondsBank++;
            EmipokemonConfig.BalanceSettings balance = settings();
            if (data.economy.activeSecondsBank >= balance.activeRewardSeconds) {
                data.economy.activeSecondsBank -= balance.activeRewardSeconds;
                long paid = credit(player, balance.activeRewardCoins, "active_time");
                if (paid > 0) player.sendMessage(Text.literal("§d🐾 +" + paid + " Michicoins §7por jugar activamente"), true);
            }
        }

        Identifier biome = player.getWorld().getBiome(player.getBlockPos()).getKey()
                .map(key -> key.getValue()).orElse(null);
        if (biome != null) discoverBiome(player, biome.toString());
        refreshAggregateObjectives(player);
        refreshCobbleverseObjective(player);
    }

    private void onStarter(StarterChosenEvent event) {
        ServerPlayerEntity player = reflectedPlayer(event, "getPlayer");
        if (player == null) return;
        markActive(player);
        discoverSpecies(player, event.getPokemon());
        recordObjective(player, "starter", 1L, false);
    }

    private void onCapture(PokemonCapturedEvent event) {
        ServerPlayerEntity player = reflectedPlayer(event, "getPlayer");
        if (player == null) player = onlinePlayer(event.getPokemon().getOwnerUUID(), null);
        if (player == null) return;
        Pokemon pokemon = event.getPokemon();
        markActive(player);
        boolean newSpecies = discoverSpecies(player, pokemon);
        PokemonCatalogEntry entry = catalog.get(pokemon.getSpecies().showdownId());
        long coins = captureReward(entry);
        creditDirect(player, coins, "pokemon_capture:" + pokemon.getSpecies().showdownId());
        rewardJobAction(player, JobType.CAPTURER, newSpecies ? 10L : 5L, newSpecies ? 10L : 2L, "capture");
        recordObjective(player, "capture", 1L, false);
        recordObjective(player, "capture_species:cobblemon:"
                + pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT), 1L, false);
        recordObjective(player, "owned_pokemon", partySize(player), true);
        recordObjective(player, "pokemon_level", pokemon.getLevel(), true);
    }

    private void onEvolution(EvolutionCompleteEvent event) {
        Pokemon pokemon = event.getPokemon();
        UUID ownerId = pokemon.getOwnerUUID();
        ServerPlayerEntity player = onlinePlayer(ownerId, null);
        if (player == null) return;
        markActive(player);
        discoverSpecies(player, pokemon);
        creditDirect(player, 10L, "pokemon_evolution");
        rewardJobAction(player, JobType.CARETAKER, 20L, 10L, "evolution");
        recordObjective(player, "evolution", 1L, false);
        recordObjective(player, "pokemon_level", pokemon.getLevel(), true);
    }

    private void onHeal(PokemonHealedEvent event) {
        Pokemon pokemon = event.getPokemon();
        ServerPlayerEntity player = onlinePlayer(pokemon.getOwnerUUID(), null);
        if (player == null || !event.isHealed()) return;
        long now = System.currentTimeMillis();
        Long previous = lastHealEvent.put(player.getUuid(), now);
        if (previous != null && now - previous < 2_000L) return;
        markActive(player);
        rewardJobAction(player, JobType.CARETAKER, 2L, 0L, "heal");
        recordObjective(player, "heal", 1L, false);
    }

    private void onBattleVictory(BattleVictoryEvent event) {
        for (BattleActor loser : event.getLosers()) {
            for (UUID playerId : loser.getPlayerUUIDs()) {
                PlayerData losingData = dataManager.getOrLoad(playerId);
                losingData.quests.battleWinStreak = 0L;
                dataManager.saveNow(playerId);
            }
        }
        for (BattleActor winner : event.getWinners()) {
            for (UUID playerId : winner.getPlayerUUIDs()) {
                ServerPlayerEntity player = onlinePlayer(playerId, null);
                if (player == null) continue;
                markActive(player);
                boolean wild = event.getLosers().stream()
                        .anyMatch(actor -> actor.getClass().getSimpleName().contains("PokemonBattleActor"));
                if (wild) {
                    creditDirect(player, 2L, "pokemon_battle_victory");
                    rewardJobAction(player, JobType.TRAINER, 5L, 2L, "battle_victory");
                    recordObjective(player, "wild_victory", 1L, false);
                } else {
                    creditDirect(player, 8L, "trainer_battle_victory");
                    rewardJobAction(player, JobType.TRAINER, 10L, 5L, "trainer_victory");
                }
                PlayerData data = dataManager.getOrLoad(playerId);
                data.quests.battleWinStreak++;
                recordObjective(player, "win_streak", data.quests.battleWinStreak, true);
            }
        }
    }

    private void onBlockBroken(ServerPlayerEntity player, BlockState state) {
        markActive(player);
        String id = Registries.BLOCK.getId(state.getBlock()).getPath();
        if (id.contains("ore") || id.contains("deepslate_") && isMineral(id)) {
            rewardJobAction(player, JobType.MINER, 2L, 1L, "ore_break");
        } else if (isCrop(state, id)) {
            rewardJobAction(player, JobType.FARMER, 2L, 1L, "crop_harvest");
        }
    }

    private boolean isMineral(String id) {
        return id.contains("coal") || id.contains("iron") || id.contains("copper") || id.contains("gold")
                || id.contains("redstone") || id.contains("lapis") || id.contains("diamond") || id.contains("emerald");
    }

    private boolean isCrop(BlockState state, String id) {
        return state.isOf(Blocks.WHEAT) || state.isOf(Blocks.CARROTS) || state.isOf(Blocks.POTATOES)
                || state.isOf(Blocks.BEETROOTS) || state.isOf(Blocks.NETHER_WART)
                || id.contains("apricorn") || id.contains("berry") || id.contains("crop");
    }

    private boolean discoverSpecies(ServerPlayerEntity player, Pokemon pokemon) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        String species = pokemon.getSpecies().showdownId().toLowerCase(Locale.ROOT);
        boolean added = data.quests.discoveredSpecies.add(species);
        if (added) {
            recordObjective(player, "species", data.quests.discoveredSpecies.size(), true);
            if (data.jobs.isActive(JobType.CAPTURER.id())) creditDirect(player, 15L, "new_species:" + species);
        }
        return added;
    }

    private void discoverBiome(ServerPlayerEntity player, String biome) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        if (data.quests.discoveredBiomes.add(biome)) {
            rewardJobAction(player, JobType.EXPLORER, 8L, 5L, "new_biome");
            recordObjective(player, "biomes", data.quests.discoveredBiomes.size(), true);
        }
    }

    private void refreshAggregateObjectives(ServerPlayerEntity player) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        int partySize = partySize(player);
        if (partySize > 0) recordObjective(player, "starter", 1L, true);
        recordObjective(player, "owned_pokemon", partySize, true);
        int maximumLevel = 0;
        for (Pokemon pokemon : party(player)) maximumLevel = Math.max(maximumLevel, pokemon.getLevel());
        recordObjective(player, "pokemon_level", maximumLevel, true);
        recordObjective(player, "species", data.quests.discoveredSpecies.size(), true);
        recordObjective(player, "biomes", data.quests.discoveredBiomes.size(), true);
        int jobLevel = 1;
        for (JobType job : JobType.values()) jobLevel = Math.max(jobLevel, JobType.levelFor(data.jobs.xp(job.id())));
        recordObjective(player, "job_level", jobLevel, true);
    }

    private void rewardJobAction(ServerPlayerEntity player, JobType required, long xp, long coins, String action) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        if (!data.jobs.isActive(required.id())) return;
        long before = data.jobs.xp(required.id());
        int oldLevel = JobType.levelFor(before);
        long scaledXp = settings().scaled(xp, settings().jobXpMultiplier);
        long scaledCoins = settings().scaled(coins, settings().jobCoinMultiplier);
        data.jobs.experience.merge(required.id(), scaledXp, Long::sum);
        data.jobs.completedActions.merge(action, 1L, Long::sum);
        if (scaledCoins > 0L) credit(player, scaledCoins, "job:" + required.id() + ":" + action);
        int newLevel = JobType.levelFor(data.jobs.xp(required.id()));
        if (newLevel > oldLevel) {
            player.sendMessage(Text.literal("§d✦ " + required.displayName() + " subió a nivel " + newLevel + "!"), false);
            credit(player, settings().scaled(20L * newLevel, settings().jobCoinMultiplier),
                    "job_level:" + required.id() + ":" + newLevel);
        }
        dataManager.saveNow(player.getUuid());
    }

    private void recordObjective(ServerPlayerEntity player, String type, long amount, boolean absolute) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        boolean changed = false;
        for (String track : List.of(QuestDefinition.PROGRESSION, QuestDefinition.ADVENTURE)) {
            QuestDefinition active = activeQuest(data, track);
            if (active == null || !active.objectiveType().equals(type)) continue;
            long old = data.quests.objectives.getOrDefault(active.id(), 0L);
            long next = absolute ? Math.max(old, amount) : old + amount;
            next = Math.min(next, active.target());
            if (next == old) continue;
            data.quests.objectives.put(active.id(), next);
            changed = true;
            if (next >= active.target() && data.quests.completed.add(active.id())) {
                player.sendMessage(Text.literal("§d📖 Misión lista para reclamar: §f" + active.title()), false);
            }
        }
        if (changed) dataManager.saveNow(player.getUuid());
    }

    public void recordExactAltarInvocation(ServerPlayerEntity player, Identifier altarId) {
        if (player == null || altarId == null) return;
        markActive(player);
        recordObjective(player, "altar:" + altarId, 1L, false);
    }

    private void refreshCobbleverseObjective(ServerPlayerEntity player) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        for (String track : List.of(QuestDefinition.PROGRESSION, QuestDefinition.ADVENTURE)) {
            QuestDefinition active = activeQuest(data, track);
            if (active == null) continue;
            String objective = active.objectiveType();
            if (objective.startsWith("structure:")) {
                Identifier id = Identifier.tryParse(objective.substring("structure:".length()));
                if (id != null && isInsideExactStructure(player, id)) {
                    recordObjective(player, objective, 1L, true);
                }
            } else if (objective.startsWith("advancement:")) {
                Identifier id = Identifier.tryParse(objective.substring("advancement:".length()));
                if (id != null && hasCompletedAdvancement(player, id)) {
                    recordObjective(player, objective, 1L, true);
                }
            }
        }
    }

    private boolean isInsideExactStructure(ServerPlayerEntity player, Identifier structureId) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Structure structure = world.getRegistryManager()
                .get(RegistryKeys.STRUCTURE).get(structureId);
        if (structure == null) return false;
        StructureStart start = world.getStructureAccessor()
                .getStructureContaining(player.getBlockPos(), structure);
        return start != null && start.hasChildren()
                && start.getBoundingBox().contains(player.getBlockPos());
    }

    private boolean hasCompletedAdvancement(ServerPlayerEntity player, Identifier advancementId) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        AdvancementEntry advancement = server.getAdvancementLoader().get(advancementId);
        return advancement != null && player.getAdvancementTracker().getProgress(advancement).isDone();
    }

    private QuestDefinition activeQuest(PlayerData data, String track) {
        return QuestCatalog.all().stream()
                .filter(quest -> quest.track().equals(track))
                .filter(quest -> !data.quests.claimed.contains(quest.id()))
                .findFirst().orElse(null);
    }

    private String normalizeTrack(String track) {
        return QuestDefinition.ADVENTURE.equalsIgnoreCase(track)
                ? QuestDefinition.ADVENTURE : QuestDefinition.PROGRESSION;
    }

    private boolean isQuestComplete(PlayerData data, QuestDefinition quest) {
        return objectiveValue(data, quest) >= quest.target();
    }

    private long objectiveValue(PlayerData data, QuestDefinition quest) {
        return Math.min(quest.target(), data.quests.objectives.getOrDefault(quest.id(), 0L));
    }

    private String objectiveLabel(QuestDefinition quest) {
        return switch (quest.objectiveType()) {
            case "starter" -> "Obtén tu Pokémon inicial";
            case "heal" -> "Cura a tu equipo";
            case "craft_pokeball" -> "Fabrica Poké Balls";
            case "capture" -> "Captura Pokémon";
            case "wild_victory" -> "Gana combates salvajes";
            case "owned_pokemon" -> "Pokémon en tu equipo";
            case "evolution" -> "Evoluciona Pokémon";
            case "species" -> "Especies diferentes";
            case "pokemon_level" -> "Nivel máximo del equipo";
            case "win_streak" -> "Victorias consecutivas";
            case "biomes" -> "Biomas descubiertos";
            case "job_level" -> "Nivel máximo de trabajo";
            default -> {
                if (quest.objectiveType().startsWith("advancement:")) yield "Victoria oficial";
                if (quest.objectiveType().startsWith("structure:")) yield "Estructura visitada";
                if (quest.objectiveType().startsWith("altar:")) yield "Invocación aceptada";
                if (quest.objectiveType().startsWith("capture_species:")) yield "Pokémon legendario capturado";
                yield quest.objectiveType();
            }
        };
    }

    private long credit(ServerPlayerEntity player, long requested, String reason) {
        if (requested <= 0L) return 0L;
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        long amount = requested;
        data.economy.michicoins = Math.addExact(data.economy.michicoins, amount);
        data.economy.lifetimeEarned = Math.addExact(data.economy.lifetimeEarned, amount);
        dataManager.saveNow(player.getUuid());
        audit(player.getUuid(), amount, data.economy.michicoins, reason);
        return amount;
    }

    private long creditDirect(ServerPlayerEntity player, long base, String reason) {
        return credit(player, settings().scaled(base, settings().directCoinMultiplier), reason);
    }

    private EmipokemonConfig.BalanceSettings settings() {
        return configManager.get().balance;
    }

    private boolean debit(ServerPlayerEntity player, long amount, String reason) {
        PlayerData data = dataManager.getOrLoad(player.getUuid());
        if (amount < 0L || data.economy.michicoins < amount) return false;
        data.economy.michicoins -= amount;
        data.economy.lifetimeSpent += amount;
        dataManager.saveNow(player.getUuid());
        audit(player.getUuid(), -amount, data.economy.michicoins, reason);
        return true;
    }

    private void enforceJobLimit(ServerPlayerEntity player, PlayerData data) {
        data.jobs.normalize();
        int previousSize = data.jobs.activeJobs.size();
        data.jobs.activeJobs.removeIf(jobId -> JobType.byId(jobId) == null);
        int limit = JobAccessPolicy.maxActiveJobs(player);
        boolean changed = data.jobs.activeJobs.size() != previousSize;
        Iterator<String> jobs = data.jobs.activeJobs.iterator();
        int index = 0;
        while (jobs.hasNext()) {
            jobs.next();
            if (++index > limit) {
                jobs.remove();
                changed = true;
            }
        }
        if (changed) dataManager.saveNow(player.getUuid());
    }

    private void giveItem(ServerPlayerEntity player, QuestDefinition.RewardItem reward) {
        Identifier id = Identifier.tryParse(reward.itemId());
        if (id == null) return;
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            Emipokemon.LOGGER.warn("Quest reward item {} does not exist in the current pack", reward.itemId());
            return;
        }
        ItemStack stack = new ItemStack(item, reward.count());
        if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) player.dropItem(stack, false);
    }

    private long captureReward(PokemonCatalogEntry entry) {
        if (entry == null) return 4L;
        return switch (entry.tier()) {
            case COMMON -> 4L;
            case UNCOMMON -> 6L;
            case RARE -> 10L;
            case EPIC -> 18L;
            case LEGENDARY -> 35L;
            case MYTHICAL, SPECIAL -> 50L;
        };
    }

    private PlayerPartyStore party(ServerPlayerEntity player) {
        try {
            Object storage = Cobblemon.INSTANCE.getStorage();
            for (java.lang.reflect.Method method : storage.getClass().getMethods()) {
                if (method.getName().equals("getParty") && method.getParameterCount() == 1) {
                    Object result = method.invoke(storage, player);
                    if (result instanceof PlayerPartyStore party) return party;
                }
            }
            throw new IllegalStateException("Compatible Cobblemon getParty method was not found");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not access the player's Cobblemon party", exception);
        }
    }

    private int partySize(ServerPlayerEntity player) {
        try {
            return party(player).size();
        } catch (Exception exception) {
            return 0;
        }
    }

    private ServerPlayerEntity onlinePlayer(UUID id, ServerPlayerEntity fallback) {
        if (fallback != null) return fallback;
        if (id == null) return null;
        for (ActivitySession session : sessions.values()) {
            if (id.equals(session.playerId) && session.lastPlayer != null) return session.lastPlayer;
        }
        return null;
    }

    private ServerPlayerEntity reflectedPlayer(Object owner, String methodName) {
        try {
            Object value = owner.getClass().getMethod(methodName).invoke(owner);
            return value instanceof ServerPlayerEntity player ? player : null;
        } catch (Exception exception) {
            Emipokemon.LOGGER.warn("Could not resolve Cobblemon player from {}", owner.getClass().getSimpleName());
            return null;
        }
    }

    private String reflectedText(Object owner, String methodName) {
        try {
            Object text = owner.getClass().getMethod(methodName).invoke(owner);
            Object value = text.getClass().getMethod("getString").invoke(text);
            return String.valueOf(value);
        } catch (Exception exception) {
            return String.valueOf(owner);
        }
    }

    private void markActive(ServerPlayerEntity player) {
        ActivitySession session = sessions.computeIfAbsent(player.getUuid(), ignored -> ActivitySession.at(player));
        session.lastActiveAt = System.currentTimeMillis();
        session.lastPlayer = player;
        session.playerId = player.getUuid();
    }

    private void audit(UUID playerId, long delta, long balance, String reason) {
        try {
            Files.createDirectories(ledger.getParent());
            String line = System.currentTimeMillis() + "\t" + playerId + "\t" + delta + "\t" + balance + "\t" + reason + "\n";
            Files.writeString(ledger, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not write Michicoins transaction audit", exception);
        }
    }

    public List<String> auditTail(int maximumLines) {
        int limit = Math.clamp(maximumLines, 1, 200);
        try {
            if (Files.notExists(ledger)) return List.of("Todavía no hay transacciones registradas.");
            List<String> lines = Files.readAllLines(ledger, StandardCharsets.UTF_8);
            return List.copyOf(lines.subList(Math.max(0, lines.size() - limit), lines.size()));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not read Michicoins transaction audit", exception);
            return List.of("No se pudo leer la auditoría.");
        }
    }

    private static final class ActivitySession {
        private UUID playerId;
        private ServerPlayerEntity lastPlayer;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private long lastActiveAt = System.currentTimeMillis();

        private static ActivitySession at(ServerPlayerEntity player) {
            ActivitySession session = new ActivitySession();
            session.playerId = player.getUuid();
            session.lastPlayer = player;
            session.x = player.getX();
            session.y = player.getY();
            session.z = player.getZ();
            session.yaw = player.getYaw();
            session.pitch = player.getPitch();
            return session;
        }

        private boolean updateMovement(ServerPlayerEntity player) {
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            float dyaw = Math.abs(player.getYaw() - yaw);
            float dpitch = Math.abs(player.getPitch() - pitch);
            x = player.getX(); y = player.getY(); z = player.getZ();
            yaw = player.getYaw(); pitch = player.getPitch();
            lastPlayer = player;
            return dx * dx + dy * dy + dz * dz > 0.04D || dyaw > 2.0F || dpitch > 2.0F;
        }
    }
}
