package com.emipokemon.command;

import com.emipokemon.config.ConfigManager;
import com.emipokemon.config.EmipokemonConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public final class HubCommands {
    private HubCommands() {
    }

    public static void register(ConfigManager configManager) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher, configManager));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, ConfigManager configManager) {
        dispatcher.register(literal("hub").executes(context -> teleportToHub(context.getSource(), configManager)));
        dispatcher.register(literal("spawn").executes(context -> teleportToHub(context.getSource(), configManager)));

        dispatcher.register(literal("emipokemon")
                .then(literal("hub")
                        .then(literal("setspawn")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> setSpawn(context.getSource(), configManager)))
                        .then(literal("pos1")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> setCorner(context.getSource(), configManager, true)))
                        .then(literal("pos2")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> setCorner(context.getSource(), configManager, false)))
                        .then(literal("enable")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> setEnabled(context.getSource(), configManager, true)))
                        .then(literal("disable")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> setEnabled(context.getSource(), configManager, false)))
                        .then(literal("info")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> showInfo(context.getSource(), configManager)))));
    }

    private static int setSpawn(ServerCommandSource source, ConfigManager configManager) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;

        String worldId = player.getServerWorld().getRegistryKey().getValue().toString();
        boolean saved = configManager.update(config -> {
            if (!config.hub.world.equals(worldId)) {
                config.hub.pos1 = null;
                config.hub.pos2 = null;
            }
            config.hub.enabled = true;
            config.hub.world = worldId;
            config.hub.x = player.getX();
            config.hub.y = player.getY();
            config.hub.z = player.getZ();
            config.hub.yaw = player.getYaw();
            config.hub.pitch = player.getPitch();
        });
        if (!saved) {
            source.sendError(Text.literal("No se pudo guardar el spawn del Hub."));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Hub: spawn guardado en " + worldId + " ("
                + format(player.getX()) + ", " + format(player.getY()) + ", " + format(player.getZ()) + ")."), true);
        return 1;
    }

    private static int setCorner(ServerCommandSource source, ConfigManager configManager, boolean first) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;

        String worldId = player.getServerWorld().getRegistryKey().getValue().toString();
        EmipokemonConfig.HubSettings current = configManager.get().hub;
        if ((current.pos1 != null || current.pos2 != null) && !current.world.equals(worldId)) {
            source.sendError(Text.literal("Las dos esquinas del Hub deben estar en el mismo mundo. Usa setspawn en este mundo o desactiva y vuelve a marcar la región."));
            return 0;
        }

        BlockPos pos = player.getBlockPos();
        boolean saved = configManager.update(config -> {
            config.hub.world = worldId;
            EmipokemonConfig.RegionCorner corner = new EmipokemonConfig.RegionCorner(pos.getX(), pos.getY(), pos.getZ());
            if (first) config.hub.pos1 = corner;
            else config.hub.pos2 = corner;
        });
        if (!saved) {
            source.sendError(Text.literal("No se pudo guardar la esquina del Hub."));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Hub: pos" + (first ? "1" : "2") + " = "
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " en " + worldId + "."), true);
        return 1;
    }

    private static int setEnabled(ServerCommandSource source, ConfigManager configManager, boolean enabled) {
        EmipokemonConfig.HubSettings hub = configManager.get().hub;
        if (enabled && !hub.hasRegion()) {
            source.sendError(Text.literal("Marca primero /emipokemon hub pos1 y pos2."));
            return 0;
        }
        boolean saved = configManager.update(config -> config.hub.enabled = enabled);
        if (!saved) {
            source.sendError(Text.literal("No se pudo guardar el estado del Hub."));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Hub: región " + (enabled ? "activada" : "desactivada") + "."), true);
        return 1;
    }

    private static int showInfo(ServerCommandSource source, ConfigManager configManager) {
        EmipokemonConfig.HubSettings hub = configManager.get().hub;
        String region = hub.hasRegion()
                ? "pos1=" + corner(hub.pos1) + " | pos2=" + corner(hub.pos2)
                : "sin límites completos";
        source.sendFeedback(() -> Text.literal("Hub " + (hub.enabled ? "ACTIVO" : "INACTIVO")
                + " | mundo=" + hub.world
                + " | spawn=" + format(hub.x) + ", " + format(hub.y) + ", " + format(hub.z)
                + " | " + region), false);
        return 1;
    }

    private static int teleportToHub(ServerCommandSource source, ConfigManager configManager) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;

        EmipokemonConfig.HubSettings hub = configManager.get().hub;
        if (!hub.enabled) {
            source.sendError(Text.literal("El Hub todavía no está configurado."));
            return 0;
        }

        Identifier worldId = Identifier.tryParse(hub.world);
        if (worldId == null) {
            source.sendError(Text.literal("El mundo configurado para el Hub no es válido: " + hub.world));
            return 0;
        }
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
        ServerWorld targetWorld = source.getServer().getWorld(worldKey);
        if (targetWorld == null) {
            source.sendError(Text.literal("El mundo del Hub no está cargado: " + hub.world));
            return 0;
        }

        player.teleport(targetWorld, hub.x, hub.y, hub.z, hub.yaw, hub.pitch);
        player.sendMessage(Text.literal("Bienvenido al Hub de Cobbleverse."), false);
        return 1;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Este comando debe ejecutarse como jugador."));
        }
        return player;
    }

    private static String corner(EmipokemonConfig.RegionCorner corner) {
        return corner.x + "," + corner.y + "," + corner.z;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
