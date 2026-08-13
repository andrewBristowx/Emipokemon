package com.emipokemon.progress.command;

import com.emipokemon.progress.JobType;
import com.emipokemon.progress.ProgressionService;
import com.emipokemon.progress.network.ProgressionNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ProgressionCommands {
    private ProgressionCommands() {
    }

    public static void register(ProgressionService service) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerAll(dispatcher, service));
    }

    private static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, ProgressionService service) {
        dispatcher.register(literal("misiones").executes(context -> open(context.getSource(), "missions")));
        dispatcher.register(literal("quests").executes(context -> open(context.getSource(), "missions")));
        dispatcher.register(literal("saldo").executes(context -> balance(context.getSource(), service)));

        var jobs = literal("jobs")
                .executes(context -> open(context.getSource(), "jobs"))
                .then(literal("leave").executes(context -> {
                    ServerPlayerEntity player = requirePlayer(context.getSource());
                    if (player == null) return 0;
                    service.leaveAllJobs(player);
                    return 1;
                }));
        for (JobType job : JobType.values()) {
            jobs.then(literal("join").then(literal(job.id()).executes(context -> {
                ServerPlayerEntity player = requirePlayer(context.getSource());
                return player != null && service.joinJob(player, job.id()) ? 1 : 0;
            })));
            jobs.then(literal("leave").then(literal(job.id()).executes(context -> {
                ServerPlayerEntity player = requirePlayer(context.getSource());
                return player != null && service.leaveJob(player, job.id()) ? 1 : 0;
            })));
        }
        dispatcher.register(jobs);

        dispatcher.register(literal("michicoins")
                .executes(context -> balance(context.getSource(), service))
                .then(literal("balance").executes(context -> balance(context.getSource(), service)))
                .then(literal("give").requires(source -> source.hasPermissionLevel(4))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("cantidad", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                                            long amount = LongArgumentType.getLong(context, "cantidad");
                                            service.adminGive(target, amount, context.getSource().getName());
                                            context.getSource().sendFeedback(() -> Text.literal("§dEntregados " + amount + " Michicoins a " + target.getName().getString()), true);
                                            return 1;
                                        }))))
                .then(literal("take").requires(source -> source.hasPermissionLevel(4))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("cantidad", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                                            long amount = LongArgumentType.getLong(context, "cantidad");
                                            boolean success = service.adminTake(target, amount, context.getSource().getName());
                                            context.getSource().sendFeedback(() -> Text.literal(success
                                                    ? "§dRetirados " + amount + " Michicoins a " + target.getName().getString()
                                                    : "§cEl jugador no tiene saldo suficiente."), true);
                                            return success ? 1 : 0;
                                        }))))
                .then(literal("set").requires(source -> source.hasPermissionLevel(4))
                        .then(argument("jugador", EntityArgumentType.player())
                                .then(argument("cantidad", LongArgumentType.longArg(0L))
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                                            long amount = LongArgumentType.getLong(context, "cantidad");
                                            service.adminSet(target, amount, context.getSource().getName());
                                            context.getSource().sendFeedback(() -> Text.literal("§dSaldo de " + target.getName().getString() + " fijado en " + amount), true);
                                            return 1;
                                        })))));

        dispatcher.register(literal("emipokemon").requires(source -> source.hasPermissionLevel(4))
                .then(literal("quests")
                        .then(literal("reset")
                                .then(argument("jugador", EntityArgumentType.player())
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                                            service.resetQuests(target);
                                            context.getSource().sendFeedback(() -> Text.literal("Misiones reiniciadas para " + target.getName().getString()), true);
                                            return 1;
                                        })))
                        .then(literal("signal")
                                .then(argument("jugador", EntityArgumentType.player())
                                        .then(argument("senal", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
                                                    String signal = StringArgumentType.getString(context, "senal");
                                                    service.signal(target, signal);
                                                    context.getSource().sendFeedback(() -> Text.literal("Señal de misión aplicada: " + signal), false);
                                                    return 1;
                                                }))))));
    }

    private static int open(ServerCommandSource source, String tab) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        ProgressionNetworking.open(player, tab);
        return 1;
    }

    private static int balance(ServerCommandSource source, ProgressionService service) {
        ServerPlayerEntity player = requirePlayer(source);
        if (player == null) return 0;
        source.sendFeedback(() -> Text.literal("§d🐾 Saldo: §f" + service.balance(player.getUuid()) + " Michicoins"), false);
        return 1;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) source.sendError(Text.literal("Este comando debe ejecutarse como jugador."));
        return player;
    }
}
