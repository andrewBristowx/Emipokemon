package com.emipokemon.command;

import com.emipokemon.data.PlayerData;
import com.emipokemon.data.PlayerDataManager;
import com.emipokemon.gacha.GachaProgress;
import com.emipokemon.gacha.GachaRollResult;
import com.emipokemon.gacha.GachaService;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.banner.BannerDefinition;
import com.emipokemon.gacha.banner.BannerManager;
import com.emipokemon.gacha.catalog.PokemonCatalogEntry;
import com.emipokemon.gacha.catalog.PokemonCatalogService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class GachaCommands {
    private GachaCommands() {
    }

    public static void register(
            PokemonCatalogService catalog,
            BannerManager banners,
            GachaService gacha,
            PlayerDataManager playerData
    ) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher, catalog, banners, gacha, playerData));
    }

    private static void register(
            CommandDispatcher<ServerCommandSource> dispatcher,
            PokemonCatalogService catalog,
            BannerManager banners,
            GachaService gacha,
            PlayerDataManager playerData
    ) {
        dispatcher.register(literal("emipokemon")
                .then(literal("gacha")
                        .then(literal("catalog")
                                .executes(context -> {
                                    Map<GachaTier, Long> counts = catalog.tierCounts();
                                    context.getSource().sendFeedback(() -> Text.literal(
                                            "Catalogo Emipokemon: " + catalog.size() + " Pokemon | " + formatCountsLong(counts)), false);
                                    return catalog.size();
                                }))
                        .then(literal("inspect")
                                .then(argument("pokemon", StringArgumentType.word())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "pokemon");
                                            PokemonCatalogEntry entry = catalog.get(id);
                                            if (entry == null) {
                                                context.getSource().sendError(Text.literal("Pokemon no encontrado en el catalogo: " + id));
                                                return 0;
                                            }
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    entry.displayName() + " | tier=" + entry.tier().name()
                                                            + " | gen=" + entry.generation()
                                                            + " | region=" + entry.region()
                                                            + " | tipos=" + entry.types()
                                                            + " | catchRate=" + entry.catchRate()
                                                            + " | BST=" + entry.baseStatTotal()
                                                            + " | labels=" + entry.labels()), false);
                                            return 1;
                                        })))
                        .then(literal("banners")
                                .executes(context -> {
                                    String names = banners.all().stream()
                                            .map(banner -> banner.id + (banner.enabled ? "" : " [OFF]"))
                                            .reduce((a, b) -> a + ", " + b).orElse("ninguno");
                                    context.getSource().sendFeedback(() -> Text.literal("Banners: " + names), false);
                                    return banners.size();
                                }))
                        .then(literal("info")
                                .then(argument("banner", StringArgumentType.word())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "banner");
                                            BannerDefinition banner = banners.get(id);
                                            if (banner == null) {
                                                context.getSource().sendError(Text.literal("Banner no encontrado: " + id));
                                                return 0;
                                            }
                                            Map<GachaTier, Integer> counts = gacha.poolCounts(id);
                                            String generations = banner.generations.isEmpty() ? "todas" : banner.generations.toString();
                                            String featured = banner.featuredSpecies.isEmpty() ? "ninguno" : banner.featuredSpecies.toString();
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    banner.displayName + " | generaciones=" + generations
                                                            + " | destacados=" + featured
                                                            + " | pool: " + formatCounts(counts)), false);
                                            return 1;
                                        })))
                        .then(literal("pity")
                                .then(argument("banner", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            if (player == null) {
                                                context.getSource().sendError(Text.literal("Este comando debe ejecutarse como jugador."));
                                                return 0;
                                            }
                                            String id = StringArgumentType.getString(context, "banner");
                                            BannerDefinition banner = banners.get(id);
                                            if (banner == null) {
                                                context.getSource().sendError(Text.literal("Banner no encontrado: " + id));
                                                return 0;
                                            }
                                            PlayerData data = playerData.getOrLoad(player.getUuid());
                                            GachaProgress progress = data.gacha(banner.id);
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Pity " + banner.displayName + ": total=" + progress.totalPulls
                                                            + " | epico=" + progress.pullsSinceEpicOrBetter + "/" + banner.pity.epicGuarantee
                                                            + " | legendario=" + progress.pullsSinceLegendaryOrBetter + "/" + banner.pity.hardLegendaryGuarantee), false);
                                            return 1;
                                        })))
                        .then(literal("simulate")
                                .requires(source -> source.hasPermissionLevel(4))
                                .then(argument("banner", StringArgumentType.word())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "banner");
                                            GachaRollResult result = gacha.simulate(id);
                                            if (result == null) {
                                                context.getSource().sendError(Text.literal("No se pudo simular el banner " + id));
                                                return 0;
                                            }
                                            context.getSource().sendFeedback(() -> Text.literal("SIMULACION: " + describe(result)), false);
                                            return 1;
                                        })))
                        .then(literal("pull")
                                .requires(source -> source.hasPermissionLevel(4))
                                .then(argument("banner", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();
                                            if (player == null) {
                                                context.getSource().sendError(Text.literal("Este comando debe ejecutarse como jugador."));
                                                return 0;
                                            }
                                            String id = StringArgumentType.getString(context, "banner");
                                            GachaService.PullOutcome outcome = gacha.pull(player, id);
                                            if (!outcome.success()) {
                                                context.getSource().sendError(Text.literal("Gacha: " + outcome.error()));
                                                return 0;
                                            }
                                            GachaRollResult result = outcome.result();
                                            context.getSource().sendFeedback(() -> Text.literal("GACHA: " + describe(result)), false);
                                            return 1;
                                        })))
                        .then(literal("reload")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> {
                                    boolean bannerOk = banners.reload();
                                    catalog.rebuild();
                                    context.getSource().sendFeedback(() -> Text.literal(
                                            bannerOk
                                                    ? "Gacha recargado: " + banners.size() + " banners, " + catalog.size() + " Pokemon."
                                                    : "Fallo al recargar banners; se conservaron los ultimos validos. Catalogo reconstruido."), false);
                                    return bannerOk ? 1 : 0;
                                }))));
    }

    private static String describe(GachaRollResult result) {
        return result.tier().name() + " -> " + result.pokemon().displayName()
                + " Nv." + result.level()
                + (result.shiny() ? " SHINY" : "")
                + (result.legendaryPityTriggered() ? " [HARD PITY]" : result.epicPityTriggered() ? " [PITY]" : "");
    }

    private static String formatCounts(Map<GachaTier, Integer> counts) {
        StringBuilder text = new StringBuilder();
        for (GachaTier tier : GachaTier.values()) {
            int count = counts.getOrDefault(tier, 0);
            if (count <= 0) continue;
            if (!text.isEmpty()) text.append(" | ");
            text.append(tier.name()).append('=').append(count);
        }
        return text.toString();
    }

    private static String formatCountsLong(Map<GachaTier, Long> counts) {
        StringBuilder text = new StringBuilder();
        for (GachaTier tier : GachaTier.values()) {
            long count = counts.getOrDefault(tier, 0L);
            if (count <= 0) continue;
            if (!text.isEmpty()) text.append(" | ");
            text.append(tier.name()).append('=').append(count);
        }
        return text.toString();
    }
}
