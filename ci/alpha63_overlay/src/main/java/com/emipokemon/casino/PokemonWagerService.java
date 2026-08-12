package com.emipokemon.casino;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.emipokemon.Emipokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative best-of-three coin flip with crash-recoverable Pokémon escrow. */
final class PokemonWagerService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESULT_MS = 8_000L;
    private final Map<Key, Session> sessions = new HashMap<>();
    private final Path escrowDirectory = Emipokemon.configManager().configDirectory().resolve("pokemon-wager-escrow");
    private final Path auditFile = Emipokemon.configManager().configDirectory().resolve("casino-audit.log");
    private boolean initialized;

    synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player, server));
        ServerLifecycleEvents.SERVER_STARTED.register(this::recoverEscrows);
    }

    synchronized void open(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        Session session = session(player, machine);
        send(player, machine, session, session.message);
    }

    synchronized void action(ServerPlayerEntity player, CasinoMachineBlockEntity machine, String rawAction) {
        Session session = session(player, machine);
        String action = rawAction == null ? "" : rawAction.strip().toLowerCase(java.util.Locale.ROOT);
        if (session.resultUntil > 0L) {
            send(player, machine, session, "Espera a que termine la presentación del resultado.");
            return;
        }
        Participant participant = session.participants.get(player.getUuid());
        if ("join".equals(action)) {
            if (participant != null) { send(player, machine, session, "Ya estás en este duelo."); return; }
            if (session.participants.size() >= 2) { send(player, machine, session, "La mesa ya tiene dos jugadores."); return; }
            PlayerPartyStore party = party(player);
            int first = firstPokemonSlot(party);
            if (first < 0) { send(player, machine, session, "Necesitas al menos un Pokémon en tu equipo."); return; }
            participant = new Participant(player.getUuid(), player.getGameProfile().getName(), first);
            session.participants.put(player.getUuid(), participant);
            session.message = participant.name + " se unió al duelo.";
            audit(player.getServer(), participant.id, "pokemon_flip:join", "slot=" + first);
            broadcast(player.getServer(), machine, session);
            return;
        }
        if (participant == null) { send(player, machine, session, "Pulsa UNIRTE antes de seleccionar un Pokémon."); return; }
        if ("cancel".equals(action)) {
            session.participants.remove(player.getUuid());
            session.message = participant.name + " abandonó la mesa antes del depósito.";
            audit(player.getServer(), participant.id, "pokemon_flip:cancel", "before_escrow");
            broadcast(player.getServer(), machine, session);
            return;
        }
        if ("previous".equals(action) || "next".equals(action)) {
            participant.slot = moveSlot(party(player), participant.slot, "next".equals(action) ? 1 : -1);
            participant.ready = false;
            send(player, machine, session, "Pokémon seleccionado. Confirma cuando estés listo.");
            return;
        }
        if (!"ready".equals(action)) { send(player, machine, session, "Acción de duelo inválida."); return; }
        Pokemon selected = selected(player, participant.slot);
        if (selected == null) { participant.ready = false; send(player, machine, session, "Ese espacio ya no contiene un Pokémon."); return; }
        participant.pokemonId = selected.getUuid();
        participant.ready = true;
        session.message = participant.name + " confirmó su Pokémon.";
        audit(player.getServer(), participant.id, "pokemon_flip:confirm", selected.getUuid().toString());
        if (session.participants.size() == 2 && session.participants.values().stream().allMatch(p -> p.ready)) resolve(player.getServer(), machine, session);
        else broadcast(player.getServer(), machine, session);
    }

    private void resolve(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        List<Participant> players = new ArrayList<>(session.participants.values());
        ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayer(players.get(0).id);
        ServerPlayerEntity secondPlayer = server.getPlayerManager().getPlayer(players.get(1).id);
        if (firstPlayer == null || secondPlayer == null) { cancelBeforeEscrow(server, machine, session, "Duelo cancelado: ambos jugadores deben seguir conectados."); return; }
        Pokemon first = selectedByUuid(firstPlayer, players.get(0).pokemonId);
        Pokemon second = selectedByUuid(secondPlayer, players.get(1).pokemonId);
        if (first == null || second == null) { cancelBeforeEscrow(server, machine, session, "Duelo cancelado: una selección cambió antes de confirmarse."); return; }
        if (first.getUuid().equals(second.getUuid())) {
            cancelBeforeEscrow(server, machine, session, "Duelo cancelado: el mismo UUID Pokémon no puede depositarse dos veces.");
            audit(server, players.get(0).id, "pokemon_flip:rejected", "duplicate_uuid=" + first.getUuid());
            return;
        }

        Escrow escrow = new Escrow();
        escrow.id = UUID.randomUUID();
        escrow.status = "PREPARED";
        escrow.firstPlayer = players.get(0).id;
        escrow.secondPlayer = players.get(1).id;
        escrow.firstPokemon = first.getUuid();
        escrow.secondPokemon = second.getUuid();
        escrow.firstNbt = savePokemon(first, server).toString();
        escrow.secondNbt = savePokemon(second, server).toString();
        writeEscrow(escrow);
        audit(server, players.get(0).id, "pokemon_flip:escrow_prepared", escrow.id + ":" + escrow.firstPokemon + ":" + escrow.secondPokemon);
        PlayerPartyStore firstParty = party(firstPlayer);
        PlayerPartyStore secondParty = party(secondPlayer);
        if (!firstParty.remove(first)) { deleteEscrow(escrow); cancelBeforeEscrow(server, machine, session, "No se pudo reservar el primer Pokémon."); return; }
        if (!secondParty.remove(second)) {
            addSafely(firstPlayer, first);
            deleteEscrow(escrow);
            cancelBeforeEscrow(server, machine, session, "No se pudo reservar el segundo Pokémon; el primero fue devuelto.");
            return;
        }
        escrow.status = "ESCROWED";
        writeEscrow(escrow);
        audit(server, players.get(0).id, "pokemon_flip:escrowed", escrow.id.toString());
        int firstWins = 0;
        int secondWins = 0;
        StringBuilder flips = new StringBuilder();
        while (firstWins < 2 && secondWins < 2) {
            boolean heads = RANDOM.nextBoolean();
            if (heads) firstWins++; else secondWins++;
            if (!flips.isEmpty()) flips.append('-');
            flips.append(heads ? 'C' : 'S');
        }
        boolean firstWon = firstWins == 2;
        escrow.winner = firstWon ? escrow.firstPlayer : escrow.secondPlayer;
        escrow.flips = flips.toString();
        writeEscrow(escrow);
        audit(server, escrow.winner, "pokemon_flip:result_committed", escrow.id + ":" + escrow.flips);
        ServerPlayerEntity winner = firstWon ? firstPlayer : secondPlayer;
        Pokemon winnerPokemon = firstWon ? first : second;
        Pokemon capturedPokemon = firstWon ? second : first;
        addSafely(winner, winnerPokemon);
        addSafely(winner, capturedPokemon);
        escrow.status = "DELIVERED";
        writeEscrow(escrow);
        audit(server, escrow.winner, "pokemon_flip:delivered", escrow.id + ":" + escrow.firstPokemon + ":" + escrow.secondPokemon);
        machine.activate();
        session.message = "Resultado " + flips + ": " + winner.getGameProfile().getName() + " ganó 2 de 3 y recibió ambos Pokémon.";
        session.lastWinner = winner.getUuid();
        session.lastFlips = flips.toString();
        session.resultUntil = System.currentTimeMillis() + RESULT_MS;
        broadcast(server, machine, session);
    }

    private void recoverEscrows(MinecraftServer server) {
        try {
            Files.createDirectories(escrowDirectory);
            try (var paths = Files.list(escrowDirectory)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> recoverEscrow(server, path));
            }
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not scan Pokémon wager escrows", exception);
        }
    }

    private void recoverEscrow(MinecraftServer server, Path path) {
        try {
            Escrow escrow = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Escrow.class);
            if (escrow == null || "DELIVERED".equals(escrow.status) || "RECOVERED".equals(escrow.status)) return;
            if ("PREPARED".equals(escrow.status) || escrow.winner == null) {
                // No authoritative result was persisted: cancel and restore original owners.
                restoreIfMissing(server, escrow.firstPlayer, escrow.firstPokemon, escrow.firstNbt);
                restoreIfMissing(server, escrow.secondPlayer, escrow.secondPokemon, escrow.secondNbt);
                escrow.status = "RECOVERED";
            } else {
                // A winner was persisted: finish the settlement. UUID checks make this idempotent
                // even if the process stopped between delivery of the first and second Pokémon.
                deliverIfMissing(server, escrow.winner, escrow.firstPokemon, escrow.firstNbt);
                deliverIfMissing(server, escrow.winner, escrow.secondPokemon, escrow.secondNbt);
                escrow.status = "DELIVERED";
            }
            writeEscrow(escrow);
            audit(server, escrow.winner == null ? escrow.firstPlayer : escrow.winner,
                    "pokemon_flip:recovered", escrow.id + ":" + escrow.status);
            Emipokemon.LOGGER.warn("Recovered interrupted Pokémon wager escrow {} without duplicating UUIDs", escrow.id);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not recover Pokémon wager escrow {}", path, exception);
        }
    }

    private void restoreIfMissing(MinecraftServer server, UUID owner, UUID pokemonId, String snbt) throws Exception {
        PlayerPartyStore party = party(owner, server);
        if (party.get(pokemonId) != null || overflowContains(party, server, pokemonId)) return;
        Pokemon pokemon = loadPokemon(StringNbtReader.parse(snbt), server);
        if (!party.add(pokemon)) overflowAdd(party, server, pokemon);
    }

    private void deliverIfMissing(MinecraftServer server, UUID winner, UUID pokemonId, String snbt) throws Exception {
        restoreIfMissing(server, winner, pokemonId, snbt);
    }

    private void addSafely(ServerPlayerEntity player, Pokemon pokemon) {
        PlayerPartyStore party = party(player);
        if (!party.add(pokemon)) overflowAdd(party, player.getServer(), pokemon);
    }

    private NbtCompound savePokemon(Pokemon pokemon, MinecraftServer server) {
        try {
            for (var method : pokemon.getClass().getMethods()) if (method.getName().equals("saveToNBT") && method.getParameterCount() == 2) {
                Object value = method.invoke(pokemon, server.getRegistryManager(), new NbtCompound());
                if (value instanceof NbtCompound nbt) return nbt;
            }
            throw new NoSuchMethodException("Pokemon.saveToNBT");
        } catch (Exception exception) { throw new IllegalStateException("Could not serialize escrow Pokémon", exception); }
    }

    private Pokemon loadPokemon(NbtCompound nbt, MinecraftServer server) {
        try {
            Pokemon pokemon = new Pokemon();
            for (var method : pokemon.getClass().getMethods()) if (method.getName().equals("loadFromNBT") && method.getParameterCount() == 2) {
                Object value = method.invoke(pokemon, server.getRegistryManager(), nbt);
                if (value instanceof Pokemon loaded) return loaded;
            }
            throw new NoSuchMethodException("Pokemon.loadFromNBT");
        } catch (Exception exception) { throw new IllegalStateException("Could not deserialize escrow Pokémon", exception); }
    }

    private Object overflow(PlayerPartyStore party, MinecraftServer server) {
        try {
            for (var method : party.getClass().getMethods()) if (method.getName().equals("getOverflowPC") && method.getParameterCount() == 1)
                return method.invoke(party, server.getRegistryManager());
            throw new NoSuchMethodException("PlayerPartyStore.getOverflowPC");
        } catch (Exception exception) { throw new IllegalStateException("Could not access Cobblemon PC", exception); }
    }

    private boolean overflowContains(PlayerPartyStore party, MinecraftServer server, UUID pokemonId) {
        Object pc = overflow(party, server);
        if (pc instanceof Iterable<?> iterable) for (Object value : iterable)
            if (value instanceof Pokemon pokemon && pokemonId.equals(pokemon.getUuid())) return true;
        return false;
    }

    private void overflowAdd(PlayerPartyStore party, MinecraftServer server, Pokemon pokemon) {
        try {
            Object pc = overflow(party, server);
            for (var method : pc.getClass().getMethods()) if (method.getName().equals("add") && method.getParameterCount() == 1) {
                method.invoke(pc, pokemon);
                return;
            }
            throw new NoSuchMethodException("PCStore.add");
        } catch (Exception exception) { throw new IllegalStateException("Could not deliver Pokémon to PC", exception); }
    }

    private void writeEscrow(Escrow escrow) {
        try {
            Files.createDirectories(escrowDirectory);
            Path target = escrowDirectory.resolve(escrow.id + ".json");
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(escrow), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (Exception ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception exception) { throw new IllegalStateException("Could not persist Pokémon escrow", exception); }
    }

    private void deleteEscrow(Escrow escrow) {
        try { Files.deleteIfExists(escrowDirectory.resolve(escrow.id + ".json")); }
        catch (Exception exception) { Emipokemon.LOGGER.error("Could not delete unused escrow {}", escrow.id, exception); }
    }

    private PlayerPartyStore party(ServerPlayerEntity player) { return party(player.getUuid(), player.getServer()); }

    private PlayerPartyStore party(UUID playerId, MinecraftServer server) {
        try {
            Object storage = Cobblemon.INSTANCE.getStorage();
            for (var method : storage.getClass().getMethods()) {
                if (!method.getName().equals("getParty")) continue;
                Object result;
                if (method.getParameterCount() == 1) {
                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(playerId);
                    if (online == null) continue;
                    result = method.invoke(storage, online);
                } else if (method.getParameterCount() == 2) result = method.invoke(storage, playerId, server.getRegistryManager());
                else continue;
                if (result instanceof PlayerPartyStore party) return party;
            }
            throw new IllegalStateException("No compatible Cobblemon getParty method");
        } catch (Exception exception) { throw new IllegalStateException("Could not access Cobblemon storage", exception); }
    }

    private Pokemon selected(ServerPlayerEntity player, int slot) {
        PlayerPartyStore party = party(player);
        return slot >= 0 && slot < party.size() ? party.get(slot) : null;
    }

    private Pokemon selectedByUuid(ServerPlayerEntity player, UUID id) { return id == null ? null : party(player).get(id); }
    private int firstPokemonSlot(PlayerPartyStore party) { for (int i = 0; i < party.size(); i++) if (party.get(i) != null) return i; return -1; }
    private int moveSlot(PlayerPartyStore party, int start, int direction) {
        int size = Math.max(1, party.size());
        for (int step = 1; step <= size; step++) { int slot = Math.floorMod(start + direction * step, size); if (party.get(slot) != null) return slot; }
        return start;
    }

    private Session session(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        Key key = new Key(player.getWorld().getRegistryKey().getValue().toString(), machine.getPos().asLong());
        return sessions.computeIfAbsent(key, ignored -> new Session());
    }

    private void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.resultUntil == 0L || now < session.resultUntil) return false;
            return true;
        });
    }

    private void disconnect(ServerPlayerEntity player, MinecraftServer server) {
        sessions.values().forEach(session -> {
            if (session.resultUntil == 0L && session.participants.remove(player.getUuid()) != null) {
                session.message = player.getGameProfile().getName() + " salió; no se había retirado ningún Pokémon.";
                audit(server, player.getUuid(), "pokemon_flip:disconnect", "before_escrow");
            }
        });
    }

    private void cancelBeforeEscrow(MinecraftServer server, CasinoMachineBlockEntity machine, Session session, String message) {
        session.message = message;
        session.participants.values().forEach(p -> p.ready = false);
        broadcast(server, machine, session);
    }

    private void broadcast(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        for (Participant participant : session.participants.values()) {
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(participant.id);
            if (target != null) send(target, machine, session, session.message);
        }
    }

    private void send(ServerPlayerEntity player, CasinoMachineBlockEntity machine, Session session, String message) {
        Participant mine = session.participants.get(player.getUuid());
        List<String> names = session.participants.values().stream().map(p -> p.name + (p.ready ? " · listo" : " · seleccionando")).toList();
        String privateState;
        String phase;
        if (session.resultUntil > 0L) {
            phase = "result";
            privateState = player.getUuid().equals(session.lastWinner) ? "Ganaste el mejor de 3. Los dos Pokémon fueron entregados a tu almacenamiento." : "Perdiste el mejor de 3. Tu Pokémon fue transferido al ganador.";
        } else if (mine == null) {
            phase = "lobby";
            privateState = "Pulsa UNIRTE. No se retira ningún Pokémon hasta que ambos confirmen.";
        } else {
            phase = "selecting";
            Pokemon pokemon = selected(player, mine.slot);
            privateState = pokemon == null ? "Selección inválida" : "Tu Pokémon: " + pokemon.getSpecies().getName() + " Nv." + pokemon.getLevel() + (mine.ready ? " · CONFIRMADO" : " · pendiente");
        }
        List<CasinoNetworking.PokemonDisplay> displays = new ArrayList<>();
        for (Participant participant : session.participants.values()) {
            ServerPlayerEntity owner = player.getServer().getPlayerManager().getPlayer(participant.id);
            Pokemon pokemon = owner == null ? null : selected(owner, participant.slot);
            if (pokemon == null && owner != null && participant.pokemonId != null) pokemon = selectedByUuid(owner, participant.pokemonId);
            if (pokemon == null) {
                displays.add(new CasinoNetworking.PokemonDisplay(participant.name, "", "Sin selección", 0, participant.ready));
            } else {
                displays.add(new CasinoNetworking.PokemonDisplay(participant.name,
                        speciesIdentifier(pokemon), pokemon.getSpecies().getName(),
                        pokemon.getLevel(), participant.ready));
            }
        }
        CasinoNetworking.CasinoState state = new CasinoNetworking.CasinoState("pokemon_flip", "Cara o sello Pokémon",
                machine.getPos().asLong(), Emipokemon.progressionService().balance(player.getUuid()), 0L, 0L,
                message, phase, 0L, session.resultUntil, names,
                session.participants.size() + "/2 jugadores · mejor de 3 · depósito persistente", privateState,
                session.lastFlips.chars().map(ch -> ch == 'C' ? 1 : 0).boxed().toList(),
                List.of(), -1, -1, displays);
        CasinoNetworking.sendTable(player, machine, state);
    }

    private String speciesIdentifier(Pokemon pokemon) {
        String fallback = "cobblemon:" + pokemon.getSpecies().getName().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
        try {
            Object identifier = pokemon.getSpecies().getClass().getMethod("getResourceIdentifier")
                    .invoke(pokemon.getSpecies());
            return identifier == null ? fallback : identifier.toString();
        } catch (ReflectiveOperationException exception) {
            Emipokemon.LOGGER.debug("Cobblemon species identifier adapter used for {}", fallback);
            return fallback;
        }
    }

    private void audit(MinecraftServer server, UUID playerId, String action, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            String line = System.currentTimeMillis() + "\t" + playerId + "\t" + action + "\t" + detail + System.lineSeparator();
            Files.writeString(auditFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not write Pokémon wager audit event {}", action, exception);
        }
    }

    private record Key(String world, long pos) { }
    private static final class Session {
        final Map<UUID, Participant> participants = new java.util.LinkedHashMap<>();
        String message = "Mesa lista: se requieren dos jugadores.";
        long resultUntil;
        UUID lastWinner;
        String lastFlips = "";
    }
    private static final class Participant {
        final UUID id; final String name; int slot; UUID pokemonId; boolean ready;
        Participant(UUID id, String name, int slot) { this.id = id; this.name = name; this.slot = slot; }
    }
    private static final class Escrow {
        UUID id; String status; UUID firstPlayer; UUID secondPlayer; UUID firstPokemon; UUID secondPokemon;
        String firstNbt; String secondNbt; UUID winner; String flips;
    }
}
