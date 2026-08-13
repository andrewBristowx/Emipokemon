package com.emipokemon.casino;

import com.emipokemon.Emipokemon;
import com.emipokemon.registry.ModRegistries;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative five-lane claw game backed only by real Pokeblocks Pokédoll items. */
final class ClawGameService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LANE_COUNT = 5;
    private final Map<Key, Session> sessions = new HashMap<>();
    private final Path operationDirectory = Emipokemon.configManager().configDirectory().resolve("claw-operations");
    private final Path auditFile = Emipokemon.configManager().configDirectory().resolve("casino-audit.log");
    private boolean initialized;

    synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTED.register(this::recoverCommittedOperations);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> recoverForPlayer(handler.player));
    }

    synchronized void open(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        Session session = sessions.computeIfAbsent(key(player, machine), ignored -> createSession());
        send(player, machine, session);
    }

    synchronized void action(ServerPlayerEntity player, CasinoMachineBlockEntity machine, String rawAction) {
        Session session = sessions.computeIfAbsent(key(player, machine), ignored -> createSession());
        String action = rawAction == null ? "" : rawAction.strip().toLowerCase(java.util.Locale.ROOT);
        if ("claw_reset".equals(action)) {
            sessions.put(key(player, machine), createSession());
            send(player, machine, sessions.get(key(player, machine)));
            return;
        }
        if (session.prizes.isEmpty()) {
            session.phase = "blocked";
            session.message = "No hay Pokédolls válidos. Revisa Pokeblocks y la configuración del servidor.";
            audit(player, "claw:blocked", "no_valid_pokeblocks_pokedoll_ids");
            send(player, machine, session);
            return;
        }
        if (!"aiming".equals(session.phase)) {
            send(player, machine, session);
            return;
        }
        if ("claw_left".equals(action)) {
            session.lane = Math.max(0, session.lane - 1);
            session.message = "Mueve la garra sobre el Pokédoll que quieres atrapar.";
            send(player, machine, session);
            return;
        }
        if ("claw_right".equals(action)) {
            session.lane = Math.min(session.prizes.size() - 1, session.lane + 1);
            session.message = "Mueve la garra sobre el Pokédoll que quieres atrapar.";
            send(player, machine, session);
            return;
        }
        if (!"claw_drop".equals(action)) {
            session.message = "Usa IZQUIERDA, BAJAR GARRA o DERECHA.";
            send(player, machine, session);
            return;
        }

        Identifier prizeId = Identifier.tryParse(session.prizes.get(session.lane));
        Item prize = validPrize(prizeId);
        if (prize == null) {
            session.phase = "blocked";
            session.message = "El premio seleccionado dejó de estar disponible; no se consumió ningún ticket.";
            audit(player, "claw:blocked", "selected_prize_missing:" + session.prizes.get(session.lane));
            send(player, machine, session);
            return;
        }
        if (!hasOne(player, ModRegistries.CLAW_TICKET)) {
            session.message = "Necesitas 1 ticket de garra. El ticket normal del gacha no sirve aquí.";
            audit(player, "claw:rejected", "missing_claw_ticket");
            send(player, machine, session);
            return;
        }

        Operation operation = new Operation();
        operation.id = UUID.randomUUID();
        operation.player = player.getUuid();
        operation.prizeId = prizeId.toString();
        operation.status = "PREPARED";
        operation.createdAt = System.currentTimeMillis();
        try {
            writeOperation(operation);
        } catch (IllegalStateException exception) {
            session.message = "No se pudo guardar la jugada; no se consumió ningún ticket.";
            audit(player, "claw:persistence_failed", operation.id + ":prepare");
            send(player, machine, session);
            return;
        }
        if (!removeOne(player, ModRegistries.CLAW_TICKET)) {
            operation.status = "CANCELLED";
            tryWriteOperation(operation);
            session.message = "El ticket cambió antes de iniciar la jugada; no se entregó ningún premio.";
            audit(player, "claw:rejected", operation.id + ":ticket_changed");
            send(player, machine, session);
            return;
        }
        operation.status = "COMMITTED";
        // The authoritative operation is durably committed before the prize is delivered.
        // A committed operation is recovered on the player's next login after a crash.
        try {
            writeOperation(operation);
        } catch (IllegalStateException exception) {
            deliver(player, new ItemStack(ModRegistries.CLAW_TICKET));
            operation.status = "REFUNDED";
            tryWriteOperation(operation);
            session.message = "No se pudo confirmar la jugada; el ticket fue devuelto.";
            audit(player, "claw:refunded", operation.id + ":commit_failed");
            send(player, machine, session);
            return;
        }
        deliver(player, new ItemStack(prize));
        operation.status = "DELIVERED";
        writeOperation(operation);

        session.phase = "caught";
        session.caughtLane = session.lane;
        session.message = "¡Atrapaste " + prize.getName().getString() + "! El ítem real fue entregado.";
        machine.activate();
        audit(player, "claw:delivered", operation.id + ":" + prizeId);
        send(player, machine, session);
    }

    private Session createSession() {
        List<String> available = configuredPrizes();
        Collections.shuffle(available, RANDOM);
        Session session = new Session();
        for (int index = 0; index < Math.min(LANE_COUNT, available.size()); index++) session.prizes.add(available.get(index));
        if (session.prizes.isEmpty()) {
            session.phase = "blocked";
            session.message = "No se encontraron Pokédolls reales de Pokeblocks.";
        } else {
            session.lane = session.prizes.size() / 2;
            session.message = "Elige un Pokédoll real, coloca la garra encima y pulsa BAJAR GARRA.";
        }
        return session;
    }

    private List<String> configuredPrizes() {
        List<String> configured = Emipokemon.configManager().get().casino.clawPlushieIds;
        if (configured == null) return new ArrayList<>();
        return configured.stream().map(Identifier::tryParse).filter(java.util.Objects::nonNull)
                .filter(id -> "pokeblocks".equals(id.getNamespace()) && id.getPath().startsWith("pokedoll_"))
                .filter(id -> validPrize(id) != null).map(Identifier::toString).distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Item validPrize(Identifier id) {
        if (id == null || !"pokeblocks".equals(id.getNamespace()) || !id.getPath().startsWith("pokedoll_")) return null;
        if (!Registries.ITEM.containsId(id)) return null;
        Item item = Registries.ITEM.get(id);
        return item == net.minecraft.item.Items.AIR ? null : item;
    }

    private boolean removeOne(ServerPlayerEntity player, Item item) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(item) || stack.isEmpty()) continue;
            stack.decrement(1);
            player.getInventory().markDirty();
            return true;
        }
        return false;
    }

    private boolean hasOne(ServerPlayerEntity player, Item item) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) return true;
        }
        return false;
    }

    private void deliver(ServerPlayerEntity player, ItemStack stack) {
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
    }

    private void recoverCommittedOperations(MinecraftServer server) {
        try {
            Files.createDirectories(operationDirectory);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) recoverForPlayer(player);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not initialize claw operation recovery", exception);
        }
    }

    private void recoverForPlayer(ServerPlayerEntity player) {
        try {
            Files.createDirectories(operationDirectory);
            try (var paths = Files.list(operationDirectory)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                    try {
                        Operation operation = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Operation.class);
                        if (operation == null || !player.getUuid().equals(operation.player) || !"COMMITTED".equals(operation.status)) return;
                        Item prize = validPrize(Identifier.tryParse(operation.prizeId));
                        if (prize == null) {
                            deliver(player, new ItemStack(ModRegistries.CLAW_TICKET));
                            operation.status = "REFUNDED";
                            audit(player, "claw:recovered_refund", operation.id + ":" + operation.prizeId);
                        } else {
                            deliver(player, new ItemStack(prize));
                            operation.status = "DELIVERED";
                            audit(player, "claw:recovered_delivery", operation.id + ":" + operation.prizeId);
                        }
                        writeOperation(operation);
                    } catch (Exception exception) {
                        Emipokemon.LOGGER.error("Could not recover claw operation {}", path, exception);
                    }
                });
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not scan claw operations for {}", player.getUuid(), exception);
        }
    }

    private void writeOperation(Operation operation) {
        try {
            Files.createDirectories(operationDirectory);
            Path target = operationDirectory.resolve(operation.id + ".json");
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(operation), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist claw operation", exception);
        }
    }

    private void tryWriteOperation(Operation operation) {
        try {
            writeOperation(operation);
        } catch (IllegalStateException exception) {
            Emipokemon.LOGGER.error("Could not persist fallback state for claw operation {}", operation.id, exception);
        }
    }

    private void audit(ServerPlayerEntity player, String action, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            String line = System.currentTimeMillis() + "\t" + player.getUuid() + "\t" + action + "\t" + detail + System.lineSeparator();
            Files.writeString(auditFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not write claw audit event {}", action, exception);
        }
    }

    private void send(ServerPlayerEntity player, CasinoMachineBlockEntity machine, Session session) {
        var settings = Emipokemon.configManager().get().casino;
        CasinoNetworking.CasinoState state = new CasinoNetworking.CasinoState("claw", "Máquina de garra",
                machine.getPos().asLong(), Emipokemon.progressionService().balance(player.getUuid()),
                settings.minimumBet, settings.maximumBet, session.message, session.phase, 0L, 0L,
                List.of(player.getGameProfile().getName()),
                session.prizes.isEmpty() ? "Sin premios válidos" : session.prizes.size() + " Pokédolls reales disponibles",
                "Posición " + (session.lane + 1) + " de " + Math.max(1, session.prizes.size()), List.of(),
                List.copyOf(session.prizes), session.lane, session.caughtLane, List.of());
        CasinoNetworking.sendTable(player, machine, state);
    }

    private Key key(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        return new Key(player.getWorld().getRegistryKey().getValue().toString(), machine.getPos().asLong(), player.getUuid());
    }

    private record Key(String world, long pos, UUID player) { }
    private static final class Session {
        final List<String> prizes = new ArrayList<>();
        int lane;
        int caughtLane = -1;
        String phase = "aiming";
        String message = "";
    }
    private static final class Operation {
        UUID id;
        UUID player;
        String prizeId;
        String status;
        long createdAt;
    }
}
