package com.emipokemon.casino;

import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class CasinoNetworking {
    private static final Gson GSON = new Gson();
    private static final CasinoService SERVICE = new CasinoService();
    private static final CasinoTableService TABLES = new CasinoTableService();
    private static final PokemonWagerService POKEMON_WAGERS = new PokemonWagerService();
    private static final ClawGameService CLAW_GAME = new ClawGameService();

    private CasinoNetworking() { }

    public static void initializeServer() {
        PayloadTypeRegistry.playS2C().register(OpenCasinoPayload.ID, OpenCasinoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CasinoActionPayload.ID, CasinoActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CasinoActionPayload.ID,
                (payload, context) -> action(context.player(), payload));
        TABLES.initialize();
        POKEMON_WAGERS.initialize();
        CLAW_GAME.initialize();
    }

    public static void open(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        if (machine.gameType() == CasinoGameType.CLAW) {
            CLAW_GAME.open(player, machine);
        } else if (machine.gameType() == CasinoGameType.POKEMON_FLIP) {
            POKEMON_WAGERS.open(player, machine);
        } else if (TABLES.handles(machine.gameType())) {
            TABLES.open(player, machine);
        } else {
            sendSingle(player, machine, "Selecciona una operación. Todas las transacciones se validan en el servidor.");
        }
    }

    private static void action(ServerPlayerEntity player, CasinoActionPayload payload) {
        BlockPos pos = BlockPos.fromLong(payload.blockPos());
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0D) {
            player.sendMessage(Text.literal("§cDebes estar cerca de la máquina."), false);
            return;
        }
        if (!(player.getWorld().getBlockEntity(pos) instanceof CasinoMachineBlockEntity machine)) {
            player.sendMessage(Text.literal("§cLa máquina ya no está disponible."), false);
            return;
        }
        if (machine.gameType() == CasinoGameType.CLAW) {
            CLAW_GAME.action(player, machine, payload.action());
            return;
        }
        if (machine.gameType() == CasinoGameType.POKEMON_FLIP) {
            POKEMON_WAGERS.action(player, machine, payload.action());
            return;
        }
        if (TABLES.handles(machine.gameType())) {
            TABLES.action(player, machine, payload.action(), payload.amount());
            return;
        }
        CasinoService.Result result = SERVICE.play(player, machine, payload.action(), payload.amount());
        sendSingle(player, machine, result.message());
    }

    private static void sendSingle(ServerPlayerEntity player, CasinoMachineBlockEntity machine, String message) {
        var settings = Emipokemon.configManager().get().casino;
        CasinoState state = new CasinoState(machine.gameType().id(), machine.gameType().displayName(),
                machine.getPos().asLong(), Emipokemon.progressionService().balance(player.getUuid()),
                settings.minimumBet, settings.maximumBet, message, "single", 0L, 0L,
                List.of(player.getGameProfile().getName()), "Operación individual.", "", List.of());
        send(player, state);
    }

    static void sendTable(ServerPlayerEntity player, CasinoMachineBlockEntity machine, CasinoState state) {
        send(player, state);
    }

    private static void send(ServerPlayerEntity player, CasinoState state) {
        if (!ServerPlayNetworking.canSend(player, OpenCasinoPayload.ID)) return;
        ServerPlayNetworking.send(player, new OpenCasinoPayload(GSON.toJson(state)));
    }

    public record CasinoState(String game, String title, long blockPos, long balance,
                              long minimumBet, long maximumBet, String message,
                              String phase, long roundId, long deadlineMillis,
                              List<String> players, String tableState, String privateState,
                              List<Integer> recentResults, List<String> itemIds,
                              int selectedIndex, int caughtIndex,
                              List<PokemonDisplay> pokemonDisplays) {
        public CasinoState(String game, String title, long blockPos, long balance,
                           long minimumBet, long maximumBet, String message,
                           String phase, long roundId, long deadlineMillis,
                           List<String> players, String tableState, String privateState,
                           List<Integer> recentResults) {
            this(game, title, blockPos, balance, minimumBet, maximumBet, message, phase,
                    roundId, deadlineMillis, players, tableState, privateState, recentResults,
                    List.of(), -1, -1, List.of());
        }
    }

    public record PokemonDisplay(String playerName, String speciesId, String speciesName,
                                 int level, boolean ready) { }

    public record OpenCasinoPayload(String json) implements CustomPayload {
        public static final Id<OpenCasinoPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "open_casino"));
        public static final PacketCodec<RegistryByteBuf, OpenCasinoPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, OpenCasinoPayload::json, OpenCasinoPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CasinoActionPayload(long blockPos, String action, long amount) implements CustomPayload {
        public static final Id<CasinoActionPayload> ID = new Id<>(Identifier.of(Emipokemon.MOD_ID, "casino_action"));
        public static final PacketCodec<RegistryByteBuf, CasinoActionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_LONG, CasinoActionPayload::blockPos,
                PacketCodecs.STRING, CasinoActionPayload::action,
                PacketCodecs.VAR_LONG, CasinoActionPayload::amount,
                CasinoActionPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
