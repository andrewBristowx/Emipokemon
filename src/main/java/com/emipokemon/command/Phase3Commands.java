package com.emipokemon.command;

import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import com.emipokemon.registry.ModRegistries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class Phase3Commands {
    private Phase3Commands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("emipokemon")
                .then(literal("give")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(literal("machine")
                                .executes(context -> give(context.getSource(), ModRegistries.STANDARD_GACHA_MACHINE.asItem(), 1, "maquina gacha"))
                                .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> give(
                                                context.getSource(),
                                                ModRegistries.STANDARD_GACHA_MACHINE.asItem(),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                "maquina gacha"
                                        ))))
                        .then(literal("ticket")
                                .then(literal("standard")
                                        .executes(context -> give(context.getSource(), ModRegistries.GACHA_TICKET, 1, "Gacha Ticket"))
                                        .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> give(
                                                        context.getSource(),
                                                        ModRegistries.GACHA_TICKET,
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        "Gacha Ticket"
                                                ))))
                                .then(literal("emi")
                                        .executes(context -> give(context.getSource(), ModRegistries.EMI_SPECIAL_BANNER_TICKET, 1, "Emi Special Banner Ticket"))
                                        .then(argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> give(
                                                        context.getSource(),
                                                        ModRegistries.EMI_SPECIAL_BANNER_TICKET,
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        "Emi Special Banner Ticket"
                                                ))))))
                .then(literal("gacha")
                        .then(literal("machine")
                                .requires(source -> source.hasPermissionLevel(4))
                                .then(literal("info")
                                        .executes(context -> withLookedAtMachine(context.getSource(), machine -> {
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Gacha Machine | banner=" + machine.getBannerId()
                                                            + " | state=" + machine.getMachineState()
                                            ), false);
                                            return 1;
                                        })))
                                .then(literal("reset")
                                        .executes(context -> withLookedAtMachine(context.getSource(), machine -> {
                                            machine.forceReset();
                                            context.getSource().sendFeedback(() -> Text.literal("Gacha Machine: estado reiniciado."), false);
                                            return 1;
                                        })))
                                .then(literal("setbanner")
                                        .then(argument("banner", StringArgumentType.word())
                                                .executes(context -> {
                                                    String bannerId = StringArgumentType.getString(context, "banner");
                                                    if (com.emipokemon.Emipokemon.bannerManager().get(bannerId) == null) {
                                                        context.getSource().sendError(Text.literal("Banner no encontrado: " + bannerId));
                                                        return 0;
                                                    }
                                                    return withLookedAtMachine(context.getSource(), machine -> {
                                                        machine.setBannerId(bannerId);
                                                        context.getSource().sendFeedback(() -> Text.literal("Gacha Machine: banner=" + bannerId), false);
                                                        return 1;
                                                    });
                                                }))))));
    }

    private static int give(ServerCommandSource source, Item item, int amount, String displayName) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Este comando debe ejecutarse como jugador."));
            return 0;
        }

        ItemStack stack = new ItemStack(item, amount);
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
        player.getInventory().markDirty();
        source.sendFeedback(() -> Text.literal("Emipokemon: recibiste " + amount + "x " + displayName + "."), false);
        return amount;
    }

    private static int withLookedAtMachine(ServerCommandSource source, MachineAction action) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Este comando debe ejecutarse como jugador."));
            return 0;
        }

        HitResult hit = player.raycast(6.0, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            source.sendError(Text.literal("Mira directamente a una maquina gacha."));
            return 0;
        }

        if (!(player.getServerWorld().getBlockEntity(blockHit.getBlockPos()) instanceof GachaMachineBlockEntity machine)) {
            source.sendError(Text.literal("El bloque seleccionado no es la base de una maquina gacha."));
            return 0;
        }
        return action.run(machine);
    }

    @FunctionalInterface
    private interface MachineAction {
        int run(GachaMachineBlockEntity machine);
    }
}
