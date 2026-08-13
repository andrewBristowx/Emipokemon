package com.emipokemon.admin;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.gacha.banner.BannerDefinition;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import com.emipokemon.hologram.HologramCommands;
import com.emipokemon.hologram.HologramRegistryStore;
import com.emipokemon.hologram.HologramService;
import com.emipokemon.shop.ShopCatalog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.literal;

public final class AdminNetworking {
    private static final Gson GSON = new GsonBuilder().create();

    private AdminNetworking() {
    }

    public static void initializeServer() {
        PayloadTypeRegistry.playS2C().register(OpenAdminPayload.ID, OpenAdminPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.ID, AdminActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.ID, (payload, context) ->
                context.server().execute(() -> accept(context.player(), payload)));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                literal("emipokemon").then(literal("admin")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;
                            open(player, "Panel administrativo abierto.");
                            return 1;
                        }))));
    }

    public static void open(ServerPlayerEntity player, String message) {
        if (!player.hasPermissionLevel(4) || !ServerPlayNetworking.canSend(player, OpenAdminPayload.ID)) return;
        ServerPlayNetworking.send(player, new OpenAdminPayload(GSON.toJson(snapshot(player)), message == null ? "" : message));
    }

    private static void accept(ServerPlayerEntity player, AdminActionPayload payload) {
        if (!player.hasPermissionLevel(4)) {
            player.sendMessage(Text.literal("§cSe requiere nivel de operador 4."), false);
            return;
        }
        String message;
        try {
            message = switch (payload.action()) {
                case "refresh" -> "Datos actualizados.";
                case "save_balance" -> saveBalance(payload.json());
                case "save_banner" -> saveBanner(payload.json());
                case "save_price" -> savePrice(payload.json());
                case "hologram_create" -> createHologram(player, payload.json());
                case "hologram_update" -> updateHologram(player, payload.json());
                case "hologram_move" -> moveHologram(player, payload.json());
                case "hologram_delete" -> deleteHologram(player, payload.json());
                case "machine_banner" -> assignBanner(player, payload.json());
                case "machine_reset" -> resetMachine(player);
                default -> throw new IllegalArgumentException("Acción administrativa desconocida");
            };
        } catch (Exception exception) {
            message = "§cNo se aplicó: " + safeMessage(exception);
        }
        Emipokemon.LOGGER.info("Admin action {} by {}: {}", payload.action(), player.getName().getString(), message.replace('§', '&'));
        open(player, message);
    }

    private static String saveBalance(String json) {
        EmipokemonConfig.BalanceSettings incoming = GSON.fromJson(json, EmipokemonConfig.BalanceSettings.class);
        if (incoming == null) throw new IllegalArgumentException("Equilibrio vacío");
        incoming.normalize();
        boolean saved = Emipokemon.configManager().update(config -> config.balance = incoming);
        if (!saved) throw new IllegalStateException("no se pudo guardar config.json");
        return "§aEquilibrio guardado en el servidor.";
    }

    private static String saveBanner(String json) {
        BannerDefinition banner = GSON.fromJson(json, BannerDefinition.class);
        if (banner == null) throw new IllegalArgumentException("Banner vacío");
        banner.normalize();
        for (var featured : banner.featuredSpecies.entrySet()) {
            if (Emipokemon.pokemonCatalog().get(featured.getKey()) == null) {
                throw new IllegalArgumentException("Pokémon destacado inexistente: " + featured.getKey());
            }
            if (!Double.isFinite(featured.getValue()) || featured.getValue() < 1.0D || featured.getValue() > 100.0D) {
                throw new IllegalArgumentException("multiplicador destacado inválido");
            }
        }
        if (Emipokemon.pokemonCatalog().all().stream().noneMatch(banner::allows)) {
            throw new IllegalArgumentException("los filtros dejan el banner sin Pokémon válidos");
        }
        if (!Emipokemon.bannerManager().save(banner)) throw new IllegalStateException("no se pudo persistir el banner");
        return "§aBanner " + banner.id + " guardado y recargado.";
    }

    private static String savePrice(String json) {
        PriceEdit edit = GSON.fromJson(json, PriceEdit.class);
        if (edit == null || edit.productId == null) throw new IllegalArgumentException("Producto vacío");
        if (!Emipokemon.shopCatalog().updatePrice(edit.productId, edit.price)) {
            throw new IllegalArgumentException("producto inexistente, protegido o precio no guardado");
        }
        return "§aPrecio guardado: " + edit.productId + ".";
    }

    private static String createHologram(ServerPlayerEntity player, String json) {
        HologramEdit edit = parseHologram(json);
        if (HologramCommands.create(player.getCommandSource(), edit.id, edit.text) == 0) {
            throw new IllegalArgumentException("no se pudo crear; revisa el ID");
        }
        apply(player, edit);
        return "§aHolograma " + edit.id + " creado.";
    }

    private static String updateHologram(ServerPlayerEntity player, String json) {
        HologramEdit edit = parseHologram(json);
        if (HologramService.record(player.getServer(), edit.id) == null) throw new IllegalArgumentException("holograma no persistido");
        apply(player, edit);
        return "§aHolograma " + edit.id + " actualizado.";
    }

    private static String moveHologram(ServerPlayerEntity player, String json) {
        HologramEdit edit = parseHologram(json);
        if (HologramService.move(player.getServer(), player.getServerWorld(), edit.id, player.getPos()) == null) {
            throw new IllegalArgumentException("holograma no persistido");
        }
        return "§aHolograma movido a tu posición.";
    }

    private static String deleteHologram(ServerPlayerEntity player, String json) {
        HologramEdit edit = parseHologram(json);
        if (!HologramService.remove(player.getServer(), edit.id)) throw new IllegalArgumentException("holograma no persistido");
        return "§eHolograma " + edit.id + " eliminado.";
    }

    private static HologramEdit parseHologram(String json) {
        HologramEdit edit = GSON.fromJson(json, HologramEdit.class);
        if (edit == null || edit.id == null || !edit.id.matches("[a-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("ID de holograma inválido");
        }
        return edit;
    }

    private static void apply(ServerPlayerEntity player, HologramEdit edit) {
        if (HologramService.updateText(player.getServer(), edit.id, edit.text == null ? "" : edit.text) == null) {
            throw new IllegalArgumentException("holograma no persistido");
        }
        HologramService.updateScale(player.getServer(), edit.id, edit.scale);
        try {
            String hex = edit.color == null ? "FFFFFF" : edit.color.replace("#", "");
            if (!hex.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException();
            HologramService.updateColor(player.getServer(), edit.id, Integer.parseUnsignedInt(hex, 16));
        } catch (Exception exception) {
            throw new IllegalArgumentException("color hexadecimal inválido");
        }
    }

    private static String assignBanner(ServerPlayerEntity player, String json) {
        BannerChoice choice = GSON.fromJson(json, BannerChoice.class);
        if (choice == null || Emipokemon.bannerManager().get(choice.bannerId) == null) {
            throw new IllegalArgumentException("banner inexistente");
        }
        GachaMachineBlockEntity machine = lookedAtMachine(player);
        machine.setBannerId(choice.bannerId);
        return "§aMáquina asignada al banner " + choice.bannerId + ".";
    }

    private static String resetMachine(ServerPlayerEntity player) {
        lookedAtMachine(player).forceReset();
        return "§aEstado de la máquina reiniciado.";
    }

    private static GachaMachineBlockEntity lookedAtMachine(ServerPlayerEntity player) {
        if (!(player.raycast(6.0D, 0.0F, false) instanceof BlockHitResult hit)
                || !(player.getServerWorld().getBlockEntity(hit.getBlockPos()) instanceof GachaMachineBlockEntity machine)) {
            throw new IllegalArgumentException("mira directamente a la base de una máquina gacha");
        }
        return machine;
    }

    private static AdminSnapshot snapshot(ServerPlayerEntity player) {
        AdminSnapshot state = new AdminSnapshot();
        state.balance = GSON.fromJson(GSON.toJson(Emipokemon.configManager().get().balance), EmipokemonConfig.BalanceSettings.class);
        state.banners.addAll(Emipokemon.bannerManager().all());
        for (ShopCatalog.Category category : Emipokemon.shopCatalog().config().categories) {
            for (ShopCatalog.Product product : category.products) {
                if (!Emipokemon.shopCatalog().forbidden(product)) {
                    state.products.add(new ProductState(product.id, category.title, product.item, product.price, product.enabled));
                }
            }
        }
        for (HologramRegistryStore.Entry entry : HologramService.records(player.getServer())) {
            state.holograms.add(new HologramState(entry.id(), entry.text(), entry.scale(),
                    String.format(Locale.ROOT, "%06X", entry.color() & 0xFFFFFF),
                    entry.world(), entry.x(), entry.y(), entry.z()));
        }
        state.audit.addAll(Emipokemon.progressionService().auditTail(60));
        state.serverTime = Instant.now().toString();
        return state;
    }

    private static String safeMessage(Exception exception) {
        String value = exception.getMessage();
        return value == null || value.isBlank() ? exception.getClass().getSimpleName() : value;
    }

    public static final class AdminSnapshot {
        public EmipokemonConfig.BalanceSettings balance = new EmipokemonConfig.BalanceSettings();
        public List<BannerDefinition> banners = new ArrayList<>();
        public List<ProductState> products = new ArrayList<>();
        public List<HologramState> holograms = new ArrayList<>();
        public List<String> audit = new ArrayList<>();
        public String serverTime = "";
    }

    public record ProductState(String id, String category, String item, long price, boolean enabled) {}
    public record HologramState(String id, String text, float scale, String color, String world,
                                 double x, double y, double z) {}

    private static final class PriceEdit { String productId; long price; }
    private static final class BannerChoice { String bannerId; }
    private static final class HologramEdit { String id; String text; float scale = 1.0F; String color = "FFFFFF"; }

    public record OpenAdminPayload(String json, String message) implements CustomPayload {
        public static final Id<OpenAdminPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_admin"));
        public static final PacketCodec<RegistryByteBuf, OpenAdminPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenAdminPayload::json,
                PacketCodecs.STRING, OpenAdminPayload::message,
                OpenAdminPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AdminActionPayload(String action, String json) implements CustomPayload {
        public static final Id<AdminActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "admin_action"));
        public static final PacketCodec<RegistryByteBuf, AdminActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, AdminActionPayload::action,
                PacketCodecs.STRING, AdminActionPayload::json,
                AdminActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
