package com.emipokemon.npc;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.emipokemon.Emipokemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcBattleService {
    private static final long START_COOLDOWN_MS = 5_000L;
    private static final Map<UUID, Long> LAST_START = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveBattle> ACTIVE_BATTLES = new ConcurrentHashMap<>();
    private static Object battleEndListener;

    private NpcBattleService() {
    }

    public static boolean auditCompatibility(MinecraftServer server) {
        if (!FabricLoader.getInstance().isModLoaded("rctapi") || !FabricLoader.getInstance().isModLoaded("rctmod")) {
            Emipokemon.LOGGER.error("Custom trainer battles disabled: exact rctapi/rctmod dependencies are missing");
            return false;
        }
        try {
            Class<?> trainerClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.Trainer");
            Class<?> trainerNpcClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC");
            Class<?> trainerPlayerClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer");
            Class<?> trainerBagClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag");
            Class<?> battleAiClass = Class.forName("com.cobblemon.mod.common.api.battles.model.ai.BattleAI");
            Class<?> rctApiClass = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi");
            Class<?> pokemonArrayClass = Array.newInstance(Pokemon.class, 0).getClass();
            trainerNpcClass.getConstructor(String.class, pokemonArrayClass, trainerBagClass,
                    battleAiClass, LivingEntity.class);
            trainerPlayerClass.getConstructor(ServerPlayerEntity.class);
            Object rct = rctApiClass.getMethod("getInstance", String.class).invoke(null, "rctmod");
            if (rct == null) throw new IllegalStateException("rctmod instance missing");
            Object battleManager = rctApiClass.getMethod("getBattleManager").invoke(rct);
            battleManager.getClass().getMethod("startSingle", trainerClass, trainerClass);
            registerBattleEndListener(rct, rctApiClass);
            Emipokemon.LOGGER.info("Exact RCT custom trainer battle API validated on {}", server.getVersion());
            return true;
        } catch (Exception | LinkageError exception) {
            Emipokemon.LOGGER.error("Custom trainer battles disabled: exact RCT API audit failed", exception);
            return false;
        }
    }

    public static List<String> validateTeam(List<String> rawSpecs) {
        if (rawSpecs == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String raw : rawSpecs) {
            if (result.size() >= 6) throw new IllegalArgumentException("El equipo admite como máximo 6 Pokémon.");
            String spec = raw == null ? "" : raw.strip();
            if (spec.isBlank()) continue;
            if (spec.length() > 256) throw new IllegalArgumentException("Cada Pokémon admite como máximo 256 caracteres.");
            PokemonProperties properties = PokemonProperties.Companion.parse(spec);
            if (!properties.hasSpecies() || PokemonSpecies.getByName(properties.getSpecies()) == null) {
                throw new IllegalArgumentException("Pokémon no válido en el equipo: " + spec);
            }
            Integer level = properties.getLevel();
            if (level != null && (level < 1 || level > 100)) {
                throw new IllegalArgumentException("El nivel debe estar entre 1 y 100: " + spec);
            }
            // Crear una copia durante la validación hace que Cobblemon compruebe forma, habilidad y movimientos.
            properties.create();
            result.add(spec);
        }
        return List.copyOf(result);
    }

    public static boolean start(ServerPlayerEntity player, ServiceNpcEntity npc) {
        if (player == null || npc == null || npc.kind() != ServiceNpcEntity.NpcKind.CUSTOM) return false;
        if (player.getWorld() != npc.getWorld() || player.squaredDistanceTo(npc) > 64.0D) {
            player.sendMessage(net.minecraft.text.Text.literal("§cDebes estar cerca del NPC para luchar."), false);
            return false;
        }
        List<String> specs;
        try {
            specs = validateTeam(npc.pokemonTeam());
        } catch (IllegalArgumentException exception) {
            player.sendMessage(net.minecraft.text.Text.literal("§cEquipo inválido: " + exception.getMessage()), false);
            return false;
        }
        if (specs.isEmpty()) {
            player.sendMessage(net.minecraft.text.Text.literal("§eEste NPC todavía no tiene equipo Pokémon."), false);
            return false;
        }
        long now = System.currentTimeMillis();
        long previous = LAST_START.getOrDefault(player.getUuid(), 0L);
        if (now - previous < START_COOLDOWN_MS) {
            player.sendMessage(net.minecraft.text.Text.literal("§eEspera unos segundos antes de iniciar otro combate."), false);
            return false;
        }
        if (!FabricLoader.getInstance().isModLoaded("rctapi") || !FabricLoader.getInstance().isModLoaded("rctmod")) {
            player.sendMessage(net.minecraft.text.Text.literal("§cCobbleverse no tiene cargados RCT API y RCT Mod."), false);
            return false;
        }
        try {
            Pokemon[] team = new Pokemon[specs.size()];
            for (int index = 0; index < specs.size(); index++) {
                team[index] = PokemonProperties.Companion.parse(specs.get(index)).create();
            }

            Class<?> trainerClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.Trainer");
            Class<?> trainerNpcClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC");
            Class<?> trainerPlayerClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerPlayer");
            Class<?> trainerBagClass = Class.forName("com.gitlab.srcmc.rctapi.api.trainer.TrainerBag");
            Class<?> battleAiClass = Class.forName("com.cobblemon.mod.common.api.battles.model.ai.BattleAI");
            Class<?> rctBattleAiClass = Class.forName("com.gitlab.srcmc.rctapi.api.ai.RCTBattleAI");
            Class<?> rctApiClass = Class.forName("com.gitlab.srcmc.rctapi.api.RCTApi");

            Object bag = trainerBagClass.getConstructor().newInstance();
            Object ai = rctBattleAiClass.getConstructor().newInstance();
            Class<?> pokemonArrayClass = Array.newInstance(Pokemon.class, 0).getClass();
            String trainerName = npc.getCustomName() == null ? npc.npcId() : npc.getCustomName().getString();
            Object npcTrainer = trainerNpcClass.getConstructor(String.class, pokemonArrayClass, trainerBagClass,
                    battleAiClass, LivingEntity.class).newInstance(trainerName, team, bag, ai, npc);
            Object playerTrainer = trainerPlayerClass.getConstructor(ServerPlayerEntity.class).newInstance(player);
            Object rct = rctApiClass.getMethod("getInstance", String.class).invoke(null, "rctmod");
            if (rct == null) throw new IllegalStateException("La instancia rctmod no está inicializada.");
            Object battleManager = rctApiClass.getMethod("getBattleManager").invoke(rct);
            boolean started = (boolean) battleManager.getClass()
                    .getMethod("startSingle", trainerClass, trainerClass)
                    .invoke(battleManager, playerTrainer, npcTrainer);
            if (started) {
                LAST_START.put(player.getUuid(), now);
                ACTIVE_BATTLES.put(player.getUuid(), new ActiveBattle(npc, System.currentTimeMillis()));
                return true;
            }
            player.sendMessage(net.minecraft.text.Text.literal("§eNo se pudo iniciar: revisa que tu equipo esté disponible y que no estés ya en combate."), false);
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            Emipokemon.LOGGER.error("Could not start exact RCT battle for NPC {}", npc.npcId(), exception);
            player.sendMessage(net.minecraft.text.Text.literal("§cNo se pudo conectar con el sistema de combates RCT."), false);
            return false;
        }
    }

    private static synchronized void registerBattleEndListener(Object rct, Class<?> rctApiClass)
            throws ReflectiveOperationException {
        if (battleEndListener != null) return;
        Class<?> eventsClass = Class.forName("com.gitlab.srcmc.rctapi.api.events.Events");
        Class<?> eventTypeClass = Class.forName("com.gitlab.srcmc.rctapi.api.events.EventType");
        Class<?> eventListenerClass = Class.forName("com.gitlab.srcmc.rctapi.api.events.EventListener");
        Object battleEnded = eventsClass.getField("BATTLE_ENDED").get(null);
        Object context = rctApiClass.getMethod("getEventContext").invoke(rct);
        Object listener = Proxy.newProxyInstance(eventListenerClass.getClassLoader(),
                new Class<?>[]{eventListenerClass}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "toString" -> "EmipokemonBattleEndListener";
                            default -> null;
                        };
                    }
                    if ("notify".equals(method.getName()) && args != null && args.length == 1) {
                        handleBattleEnded(args[0]);
                    }
                    return null;
                });
        context.getClass().getMethod("register", eventTypeClass, eventListenerClass)
                .invoke(context, battleEnded, listener);
        battleEndListener = listener;
    }

    private static void handleBattleEnded(Object event) {
        try {
            Object state = event.getClass().getMethod("getValue").invoke(event);
            Set<UUID> participants = trainerPlayerIds(state, "getParticipants1");
            participants.addAll(trainerPlayerIds(state, "getParticipants2"));
            Set<UUID> winners = trainerPlayerIds(state, "getWinners");
            Set<UUID> losingNpcs = trainerNpcIds(state, "getLosers");

            for (UUID playerId : participants) {
                ActiveBattle active = ACTIVE_BATTLES.remove(playerId);
                if (active == null || !winners.contains(playerId)
                        || !losingNpcs.contains(active.npc().getUuid())) continue;
                MinecraftServer server = active.npc().getServer();
                if (server == null) continue;
                server.execute(() -> {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                    if (player != null && !active.npc().isRemoved()) {
                        NpcRewardService.grantAfterVictory(player, active.npc());
                    }
                });
            }
            long cutoff = System.currentTimeMillis() - 3_600_000L;
            ACTIVE_BATTLES.entrySet().removeIf(entry -> entry.getValue().startedAt() < cutoff);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emipokemon.LOGGER.error("Could not process RCT battle result for custom NPC rewards", exception);
        }
    }

    private static Set<UUID> trainerPlayerIds(Object state, String methodName) throws ReflectiveOperationException {
        Set<UUID> result = new HashSet<>();
        Object value = state.getClass().getMethod(methodName).invoke(state);
        if (!(value instanceof Iterable<?> trainers)) return result;
        for (Object trainer : trainers) {
            Object entity = trainer.getClass().getMethod("getEntity").invoke(trainer);
            if (entity instanceof ServerPlayerEntity player) result.add(player.getUuid());
        }
        return result;
    }

    private static Set<UUID> trainerNpcIds(Object state, String methodName) throws ReflectiveOperationException {
        Set<UUID> result = new HashSet<>();
        Object value = state.getClass().getMethod(methodName).invoke(state);
        if (!(value instanceof Iterable<?> trainers)) return result;
        for (Object trainer : trainers) {
            Object entity = trainer.getClass().getMethod("getEntity").invoke(trainer);
            if (entity instanceof ServiceNpcEntity npc) result.add(npc.getUuid());
        }
        return result;
    }

    private record ActiveBattle(ServiceNpcEntity npc, long startedAt) {
    }
}
