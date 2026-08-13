package com.emipokemon.command;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.ConfigManager;
import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class EmipokemonCommands {
    private EmipokemonCommands() {
    }

    public static void register(ConfigManager configManager, PlayerDataManager playerDataManager) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher, configManager, playerDataManager));
    }

    private static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            ConfigManager configManager,
            PlayerDataManager playerDataManager
    ) {
        dispatcher.register(literal("emipokemon")
                .then(literal("version")
                        .executes(context -> {
                            context.getSource().sendFeedback(
                                    () -> Text.literal("Emipokemon " + Emipokemon.VERSION + " | Minecraft 1.21.1 | Cobblemon 1.7.3"),
                                    false
                            );
                            return 1;
                        }))
                .then(literal("status")
                        .executes(context -> {
                            String message = "Emipokemon OK | config v" + configManager.get().configVersion
                                    + " | player data loaded: " + playerDataManager.loadedCount();
                            context.getSource().sendFeedback(() -> Text.literal(message), false);
                            return 1;
                        }))
                .then(literal("reload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            boolean success = configManager.reload();
                            context.getSource().sendFeedback(
                                    () -> Text.literal(success
                                            ? "Emipokemon: configuracion recargada."
                                            : "Emipokemon: fallo al recargar; se conserva la ultima configuracion valida."),
                                    false
                            );
                            return success ? 1 : 0;
                        }))
                .then(literal("debug")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (source.getPlayer() == null) {
                                source.sendFeedback(() -> Text.literal("Emipokemon debug: ejecuta este subcomando como jugador."), false);
                                return 0;
                            }
                            PlayerData data = playerDataManager.getOrLoad(source.getPlayer().getUuid());
                            data.debugCounter++;
                            source.sendFeedback(
                                    () -> Text.literal("Emipokemon debugCounter=" + data.debugCounter + " para " + data.playerId),
                                    false
                            );
                            return 1;
                        })));
    }
}
