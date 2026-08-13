package com.emipokemon.shop.command;

import com.emipokemon.shop.ShopCatalog;
import com.emipokemon.shop.network.ShopNetworking;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class ShopCommands {
    private ShopCommands() {
    }

    public static void register(ShopCatalog catalog) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher, catalog));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, ShopCatalog catalog) {
        dispatcher.register(literal("tienda").executes(context -> open(context.getSource())));
        dispatcher.register(literal("pokemart").executes(context -> open(context.getSource())));
        dispatcher.register(literal("emipokemon").requires(source -> source.hasPermissionLevel(4))
                .then(literal("shop")
                        .then(literal("reload").executes(context -> {
                            boolean success = catalog.reload();
                            context.getSource().sendFeedback(() -> Text.literal(success
                                    ? "§aPoké Mart recargada: " + catalog.availableProductCount() + " productos disponibles."
                                    : "§cNo se pudo recargar; se conserva el último catálogo válido."), true);
                            return success ? 1 : 0;
                        }))
                        .then(literal("audit").executes(context -> {
                            context.getSource().sendFeedback(() -> Text.literal("§dPoké Mart: §f"
                                    + catalog.availableProductCount() + " disponibles, §7"
                                    + catalog.unavailableProductCount() + " ocultos por no existir en el modpack."), false);
                            return catalog.unavailableProductCount() == 0 ? 1 : 0;
                        }))));
    }

    private static int open(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Este comando debe ejecutarse como jugador."));
            return 0;
        }
        ShopNetworking.open(player, "balls");
        return 1;
    }
}
