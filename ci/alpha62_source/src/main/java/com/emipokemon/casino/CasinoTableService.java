package com.emipokemon.casino;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.EmipokemonConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CasinoTableService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<Integer> RED = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final long BETTING_MS = 8_000L;
    private static final long BLACKJACK_ACTION_MS = 25_000L;
    private static final long POKER_STREET_MS = 5_000L;
    private static final long RESULT_MS = 5_000L;
    private static final int MAX_BLACKJACK_PLAYERS = 5;
    private static final int MAX_POKER_PLAYERS = 6;
    private static final int MAX_ROULETTE_PLAYERS = 8;

    private final Map<TableKey, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> lastAction = new HashMap<>();
    private final Path auditFile = Emipokemon.configManager().configDirectory().resolve("casino-audit.log");
    private boolean initialized;

    synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> disconnect(handler.player, server));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::stop);
    }

    boolean handles(CasinoGameType type) {
        return type == CasinoGameType.ROULETTE || type == CasinoGameType.DICE
                || type == CasinoGameType.BLACKJACK || type == CasinoGameType.POKER;
    }

    synchronized void open(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        Session session = session(player, machine);
        session.viewers.add(player.getUuid());
        session.lastTouched = System.currentTimeMillis();
        CasinoNetworking.sendTable(player, machine, stateFor(player, session, intro(session)));
    }

    synchronized void action(ServerPlayerEntity player, CasinoMachineBlockEntity machine, String rawAction, long amount) {
        Session session = session(player, machine);
        session.viewers.add(player.getUuid());
        session.lastTouched = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long previous = lastAction.getOrDefault(player.getUuid(), 0L);
        if (now - previous < 140L) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Espera un instante antes de repetir la acción."));
            return;
        }
        lastAction.put(player.getUuid(), now);
        String action = rawAction == null ? "" : rawAction.strip().toLowerCase(java.util.Locale.ROOT);
        try {
            switch (session.type) {
                case ROULETTE -> timedBet(player, machine, session, action, amount, true);
                case DICE -> timedBet(player, machine, session, action, amount, false);
                case BLACKJACK -> blackjackAction(player, machine, session, action, amount);
                case POKER -> pokerAction(player, machine, session, action, amount);
                default -> CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Esta máquina no usa una mesa compartida."));
            }
        } catch (ArithmeticException exception) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La cantidad supera el límite seguro."));
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Shared casino table action failed at {}", machine.getPos(), exception);
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La acción fue cancelada de forma segura."));
        }
    }

    private Session session(ServerPlayerEntity player, CasinoMachineBlockEntity machine) {
        TableKey key = new TableKey(player.getWorld().getRegistryKey(), machine.getPos().asLong());
        Session existing = sessions.get(key);
        if (existing != null && existing.type == machine.gameType()) return existing;
        Session created = new Session(key, machine.gameType());
        sessions.put(key, created);
        return created;
    }

    private void timedBet(ServerPlayerEntity player, CasinoMachineBlockEntity machine, Session session,
                          String action, long amount, boolean roulette) {
        if (roulette ? !validRoulette(action) : !Set.of("under7", "over7", "exact7").contains(action)) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, roulette ? "Apuesta de ruleta inválida." : "Apuesta de dados inválida."));
            return;
        }
        if (session.phase == Phase.RESULT) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Espera a que termine la ronda actual."));
            return;
        }
        if (session.phase == Phase.IDLE) beginBetting(session);
        if (session.phase != Phase.BETTING) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La ronda ya está en juego."));
            return;
        }
        if (session.participants.containsKey(player.getUuid())) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Ya tienes una apuesta registrada en esta ronda."));
            return;
        }
        if (roulette && session.participants.size() >= MAX_ROULETTE_PLAYERS) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La mesa de ruleta está completa (8/8)."));
            return;
        }
        String game = roulette ? "roulette" : "dice";
        if (!reserve(player, amount, game + ":round:" + session.roundId)) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, betError(amount)));
            return;
        }
        Participant participant = new Participant(player.getUuid(), player.getGameProfile().getName(), amount, action);
        session.participants.put(player.getUuid(), participant);
        audit(participant.id, game + ":reserved", amount, 0L, "round=" + session.roundId + ",action=" + action);
        broadcast(player.getServer(), machine, session, participant.name + " registró una apuesta.");
    }

    private void blackjackAction(ServerPlayerEntity player, CasinoMachineBlockEntity machine, Session session,
                                 String action, long amount) {
        if ("join".equals(action) || "deal".equals(action)) {
            if (session.phase == Phase.IDLE) beginBetting(session);
            if (session.phase != Phase.BETTING) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La mano ya comenzó; espera la próxima ronda."));
                return;
            }
            if (session.participants.containsKey(player.getUuid())) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Ya estás sentado en esta ronda."));
                return;
            }
            if (session.participants.size() >= MAX_BLACKJACK_PLAYERS) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La mesa de blackjack está completa."));
                return;
            }
            if (!reserve(player, amount, "blackjack:round:" + session.roundId)) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, betError(amount)));
                return;
            }
            Participant participant = new Participant(player.getUuid(), player.getGameProfile().getName(), amount, "blackjack");
            session.participants.put(player.getUuid(), participant);
            audit(participant.id, "blackjack:reserved", amount, 0L, "round=" + session.roundId);
            broadcast(player.getServer(), machine, session, participant.name + " se sentó en la mesa.");
            return;
        }
        if (session.phase != Phase.BLACKJACK) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "No hay una mano de blackjack activa."));
            return;
        }
        Participant participant = session.participants.get(player.getUuid());
        if (participant == null) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Estás observando esta mano."));
            return;
        }
        if (participant.done) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Tu mano ya está cerrada."));
            return;
        }
        if ("hit".equals(action)) {
            participant.hand.add(draw(session));
            int value = blackjackValue(participant.hand);
            participant.lastMessage = "Pediste carta: " + cards(participant.hand) + " = " + value;
            if (value >= 21) participant.done = true;
        } else if ("stand".equals(action)) {
            participant.done = true;
            participant.lastMessage = "Te plantaste con " + blackjackValue(participant.hand) + ".";
        } else {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Acción de blackjack inválida."));
            return;
        }
        if (allDone(session)) resolveBlackjack(player.getServer(), machine, session);
        else broadcast(player.getServer(), machine, session, participant.name + " actualizó su mano.");
    }

    private void pokerAction(ServerPlayerEntity player, CasinoMachineBlockEntity machine, Session session,
                             String action, long amount) {
        if ("join".equals(action) || "deal".equals(action)) {
            if (session.phase == Phase.IDLE) beginBetting(session);
            if (session.phase != Phase.BETTING) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La ronda de póker ya comenzó."));
                return;
            }
            if (session.participants.containsKey(player.getUuid())) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Ya tienes una entrada reservada en esta ronda."));
                return;
            }
            if (session.participants.size() >= MAX_POKER_PLAYERS) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, "La mesa de póker está completa."));
                return;
            }
            long tableBet = session.participants.values().stream().findFirst().map(existing -> existing.bet).orElse(amount);
            if (!session.participants.isEmpty() && amount != tableBet) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session,
                        "Esta ronda usa una entrada única de " + tableBet + " Michicoins para que el bote sea justo."));
                return;
            }
            if (!reserve(player, amount, "poker:round:" + session.roundId)) {
                CasinoNetworking.sendTable(player, machine, stateFor(player, session, betError(amount)));
                return;
            }
            Participant participant = new Participant(player.getUuid(), player.getGameProfile().getName(), amount, "poker");
            session.participants.put(player.getUuid(), participant);
            audit(participant.id, "poker:reserved", amount, 0L, "round=" + session.roundId);
            broadcast(player.getServer(), machine, session, participant.name + " entró al bote.");
            return;
        }
        if (!session.phase.isPoker()) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "No hay una ronda de póker activa."));
            return;
        }
        Participant participant = session.participants.get(player.getUuid());
        if (participant == null) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Estás observando la ronda."));
            return;
        }
        if (!"fold".equals(action)) {
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, "Por ahora esta mesa usa entrada única: puedes continuar o retirarte."));
            return;
        }
        if (!participant.folded) {
            participant.folded = true;
            participant.done = true;
            participant.lastMessage = "Te retiraste de la ronda.";
            audit(participant.id, "poker:fold", participant.bet, 0L, "round=" + session.roundId);
        }
        if (activePokerPlayers(session) <= 1) resolvePoker(player.getServer(), machine, session);
        else broadcast(player.getServer(), machine, session, participant.name + " se retiró.");
    }

    private void beginBetting(Session session) {
        session.roundId++;
        session.phase = Phase.BETTING;
        session.deadline = System.currentTimeMillis() + BETTING_MS;
        session.participants.clear();
        session.deck.clear();
        session.dealer.clear();
        session.board.clear();
        session.revealCount = 0;
        session.publicMessage = "Apuestas abiertas.";
    }

    private boolean reserve(ServerPlayerEntity player, long bet, String reason) {
        EmipokemonConfig.CasinoSettings config = config();
        return bet >= config.minimumBet && bet <= config.maximumBet
                && Emipokemon.progressionService().spend(player, bet, "casino:" + reason + ":bet_reserved");
    }

    private String betError(long bet) {
        EmipokemonConfig.CasinoSettings config = config();
        return bet < config.minimumBet || bet > config.maximumBet
                ? "La apuesta debe estar entre " + config.minimumBet + " y " + config.maximumBet + "."
                : "No tienes suficientes Michicoins.";
    }

    private void tick(MinecraftServer server) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            for (Session session : new ArrayList<>(sessions.values())) {
                ServerWorld world = server.getWorld(session.key.world);
                CasinoMachineBlockEntity machine = null;
                if (world != null && world.getBlockEntity(BlockPos.fromLong(session.key.pos)) instanceof CasinoMachineBlockEntity found
                        && found.gameType() == session.type) machine = found;
                if (machine == null) {
                    cancel(server, session, "La mesa fue retirada; las apuestas pendientes fueron devueltas.");
                    sessions.remove(session.key);
                    continue;
                }
                pruneViewers(server, session, machine);
                if (session.phase == Phase.IDLE) {
                    if (session.viewers.isEmpty() && now - session.lastTouched > 60_000L) sessions.remove(session.key);
                    continue;
                }
                if (now < session.deadline) continue;
                switch (session.phase) {
                    case BETTING -> startAfterBetting(server, machine, session);
                    case BLACKJACK -> resolveBlackjack(server, machine, session);
                    case POKER_FLOP -> advancePoker(server, machine, session, Phase.POKER_TURN, 4, "Turn");
                    case POKER_TURN -> advancePoker(server, machine, session, Phase.POKER_RIVER, 5, "River");
                    case POKER_RIVER -> resolvePoker(server, machine, session);
                    case RESULT -> reset(server, machine, session);
                    default -> { }
                }
            }
        }
    }

    private void startAfterBetting(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        if (session.participants.isEmpty()) {
            reset(server, machine, session);
            return;
        }
        if (session.type == CasinoGameType.ROULETTE) {
            resolveRoulette(server, machine, session);
        } else if (session.type == CasinoGameType.DICE) {
            resolveDice(server, machine, session);
        } else if (session.type == CasinoGameType.BLACKJACK) {
            startBlackjack(server, machine, session);
        } else if (session.type == CasinoGameType.POKER) {
            startPoker(server, machine, session);
        }
    }

    private void resolveRoulette(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        int number = RANDOM.nextInt(37);
        String color = number == 0 ? "verde" : RED.contains(number) ? "rojo" : "negro";
        for (Participant participant : session.participants.values()) {
            boolean win = rouletteWin(participant.action, number);
            long multiplier = participant.action.startsWith("number:") ? 36L
                    : (participant.action.startsWith("dozen") || participant.action.startsWith("column")) ? 3L : 2L;
            long payout = win ? payout(participant.bet, multiplier, config().roulettePayoutMultiplier) : 0L;
            credit(participant, payout, "roulette:" + participant.action);
            participant.lastMessage = win ? "Ganaste " + payout + " Michicoins." : "Esta vez no acertaste.";
            audit(participant.id, "roulette:" + participant.action, participant.bet, payout,
                    "round=" + session.roundId + ",result=" + number + ":" + color);
        }
        machine.activate();
        session.rouletteHistory.addFirst(number);
        while (session.rouletteHistory.size() > 5) session.rouletteHistory.removeLast();
        result(session, "Ruleta: salió " + number + " " + color + ".");
        broadcast(server, machine, session, session.publicMessage);
    }

    private void resolveDice(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        int first = RANDOM.nextInt(6) + 1;
        int second = RANDOM.nextInt(6) + 1;
        int total = first + second;
        for (Participant participant : session.participants.values()) {
            boolean win = switch (participant.action) {
                case "under7" -> total < 7;
                case "over7" -> total > 7;
                default -> total == 7;
            };
            long payout = win ? payout(participant.bet, "exact7".equals(participant.action) ? 6L : 2L,
                    config().dicePayoutMultiplier) : 0L;
            credit(participant, payout, "dice:" + participant.action);
            participant.lastMessage = win ? "Ganaste " + payout + " Michicoins." : "La tirada no coincidió con tu apuesta.";
            audit(participant.id, "dice:" + participant.action, participant.bet, payout,
                    "round=" + session.roundId + ",result=" + first + "+" + second);
        }
        machine.activate();
        result(session, "Dados compartidos: " + first + " + " + second + " = " + total + ".");
        broadcast(server, machine, session, session.publicMessage);
    }

    private void startBlackjack(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        session.deck.clear();
        session.deck.addAll(shuffledDeck());
        session.dealer.clear();
        session.dealer.add(draw(session));
        session.dealer.add(draw(session));
        for (Participant participant : session.participants.values()) {
            participant.hand.clear();
            participant.hand.add(draw(session));
            participant.hand.add(draw(session));
            participant.done = blackjackValue(participant.hand) == 21;
            participant.lastMessage = participant.done ? "Blackjack natural: espera al dealer." : "Elige Pedir o Plantarse.";
        }
        session.phase = Phase.BLACKJACK;
        session.deadline = System.currentTimeMillis() + BLACKJACK_ACTION_MS;
        session.publicMessage = "Blackjack en juego. Cada jugador decide su propia mano.";
        machine.activate();
        if (allDone(session)) resolveBlackjack(server, machine, session);
        else broadcast(server, machine, session, session.publicMessage);
    }

    private void resolveBlackjack(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        if (session.phase == Phase.RESULT) return;
        for (Participant participant : session.participants.values()) participant.done = true;
        while (blackjackValue(session.dealer) < 17) session.dealer.add(draw(session));
        int dealerValue = blackjackValue(session.dealer);
        for (Participant participant : session.participants.values()) {
            int value = blackjackValue(participant.hand);
            boolean natural = participant.hand.size() == 2 && value == 21;
            boolean tie = value <= 21 && value == dealerValue;
            boolean win = value <= 21 && (dealerValue > 21 || value > dealerValue);
            long payout = tie ? payout(participant.bet, 1L, config().blackjackPayoutMultiplier)
                    : win ? (natural ? payoutDecimal(participant.bet, 2.5D, config().blackjackPayoutMultiplier)
                    : payout(participant.bet, 2L, config().blackjackPayoutMultiplier)) : 0L;
            credit(participant, payout, "blackjack");
            participant.lastMessage = "Tu mano " + value + " vs dealer " + dealerValue
                    + (tie ? " — empate, devolución " + payout : win ? " — premio " + payout : " — perdiste");
            audit(participant.id, "blackjack", participant.bet, payout,
                    "round=" + session.roundId + ",player=" + cards(participant.hand) + ",dealer=" + cards(session.dealer));
        }
        machine.activate();
        result(session, "Dealer: " + cards(session.dealer) + " = " + dealerValue + ".");
        broadcast(server, machine, session, session.publicMessage);
    }

    private void startPoker(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        if (session.participants.size() < 2) {
            for (Participant participant : session.participants.values()) {
                Emipokemon.progressionService().refund(participant.id, participant.bet, "casino:poker:lobby_cancel");
                participant.lastMessage = "Se necesitan al menos 2 jugadores. Tu entrada fue devuelta.";
                audit(participant.id, "poker:lobby_cancel", participant.bet, participant.bet, "round=" + session.roundId);
            }
            result(session, "Póker cancelado: se necesitan al menos 2 jugadores.");
            broadcast(server, machine, session, session.publicMessage);
            return;
        }
        session.deck.clear();
        session.deck.addAll(shuffledDeck());
        for (Participant participant : session.participants.values()) {
            participant.hand.clear();
            participant.hand.add(draw(session));
            participant.hand.add(draw(session));
            participant.folded = false;
            participant.done = false;
            participant.lastMessage = "Tus cartas privadas ya fueron repartidas.";
        }
        session.board.clear();
        for (int i = 0; i < 5; i++) session.board.add(draw(session));
        session.revealCount = 3;
        session.phase = Phase.POKER_FLOP;
        session.deadline = System.currentTimeMillis() + POKER_STREET_MS;
        session.publicMessage = "Flop: " + cards(session.board.subList(0, 3));
        machine.activate();
        broadcast(server, machine, session, session.publicMessage);
    }

    private void advancePoker(MinecraftServer server, CasinoMachineBlockEntity machine, Session session,
                              Phase next, int reveal, String label) {
        session.phase = next;
        session.revealCount = reveal;
        session.deadline = System.currentTimeMillis() + POKER_STREET_MS;
        session.publicMessage = label + ": " + cards(session.board.subList(0, reveal));
        broadcast(server, machine, session, session.publicMessage);
    }

    private void resolvePoker(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        if (session.phase == Phase.RESULT) return;
        List<Participant> active = session.participants.values().stream().filter(p -> !p.folded).toList();
        List<Participant> winners = new ArrayList<>();
        if (active.size() == 1) {
            winners.add(active.getFirst());
        } else if (!active.isEmpty()) {
            long best = Long.MIN_VALUE;
            for (Participant participant : active) {
                PokerHand hand = bestPoker(join(participant.hand, session.board));
                participant.pokerHand = hand;
                if (hand.score > best) {
                    winners.clear();
                    winners.add(participant);
                    best = hand.score;
                } else if (hand.score == best) {
                    winners.add(participant);
                }
            }
        }
        long pot = 0L;
        for (Participant participant : session.participants.values()) pot = Math.addExact(pot, participant.bet);
        long paidPot = payoutDecimal(pot, 1.0D, config().pokerPayoutMultiplier);
        long share = winners.isEmpty() ? 0L : paidPot / winners.size();
        long remainder = winners.isEmpty() ? 0L : paidPot % winners.size();
        Set<UUID> winnerIds = new HashSet<>();
        for (Participant winner : winners) winnerIds.add(winner.id);
        for (Participant participant : session.participants.values()) {
            long payout = winnerIds.contains(participant.id) ? share + (remainder-- > 0 ? 1L : 0L) : 0L;
            credit(participant, payout, "poker:table");
            if (participant.folded) participant.lastMessage = "Te retiraste. Bote final: " + pot + ".";
            else if (payout > 0L) participant.lastMessage = "Ganaste " + payout + " Michicoins con "
                    + (participant.pokerHand == null ? "la última mano activa" : participant.pokerHand.name) + ".";
            else participant.lastMessage = "No ganaste el bote. Tu mano: "
                    + (participant.pokerHand == null ? "sin showdown" : participant.pokerHand.name) + ".";
            audit(participant.id, "poker:table", participant.bet, payout,
                    "round=" + session.roundId + ",cards=" + cards(participant.hand) + ",board=" + cards(session.board)
                            + ",folded=" + participant.folded);
        }
        machine.activate();
        result(session, "Showdown: " + cards(session.board) + " · bote " + pot + " · ganador(es): "
                + (winners.isEmpty() ? "ninguno" : winners.stream().map(p -> p.name).reduce((a,b) -> a + ", " + b).orElse("")) + ".");
        broadcast(server, machine, session, session.publicMessage);
    }

    private void result(Session session, String message) {
        session.phase = Phase.RESULT;
        session.deadline = System.currentTimeMillis() + RESULT_MS;
        session.publicMessage = message;
    }

    private void reset(MinecraftServer server, CasinoMachineBlockEntity machine, Session session) {
        session.phase = Phase.IDLE;
        session.deadline = 0L;
        session.participants.clear();
        session.deck.clear();
        session.dealer.clear();
        session.board.clear();
        session.revealCount = 0;
        session.publicMessage = "Mesa lista para una nueva ronda.";
        session.lastTouched = System.currentTimeMillis();
        broadcast(server, machine, session, session.publicMessage);
    }

    private void disconnect(ServerPlayerEntity player, MinecraftServer server) {
        synchronized (this) {
            UUID id = player.getUuid();
            lastAction.remove(id);
            for (Session session : sessions.values()) {
                session.viewers.remove(id);
                Participant participant = session.participants.get(id);
                if (participant == null) continue;
                if (session.phase == Phase.BETTING && (session.type == CasinoGameType.BLACKJACK || session.type == CasinoGameType.POKER)) {
                    session.participants.remove(id);
                    Emipokemon.progressionService().refund(id, participant.bet, "casino:" + session.type.id() + ":left_lobby");
                    audit(id, session.type.id() + ":left_lobby", participant.bet, participant.bet, "round=" + session.roundId);
                } else if (session.phase == Phase.BLACKJACK) {
                    participant.done = true;
                    participant.lastMessage = "Desconectado: mano plantada automáticamente.";
                } else if (session.phase.isPoker()) {
                    participant.folded = true;
                    participant.done = true;
                    participant.lastMessage = "Desconectado: retirada automática.";
                }
            }
        }
    }

    private void stop(MinecraftServer server) {
        synchronized (this) {
            for (Session session : sessions.values()) {
                if (session.phase == Phase.IDLE || session.phase == Phase.RESULT) continue;
                for (Participant participant : session.participants.values()) {
                    Emipokemon.progressionService().refund(participant.id, participant.bet,
                            "casino:" + session.type.id() + ":server_stop_refund");
                    audit(participant.id, session.type.id() + ":server_stop_refund", participant.bet, participant.bet,
                            "round=" + session.roundId);
                }
            }
            sessions.clear();
            lastAction.clear();
        }
    }

    private void cancel(MinecraftServer server, Session session, String message) {
        if (session.phase != Phase.IDLE && session.phase != Phase.RESULT) {
            for (Participant participant : session.participants.values()) {
                Emipokemon.progressionService().refund(participant.id, participant.bet,
                        "casino:" + session.type.id() + ":table_removed_refund");
                audit(participant.id, session.type.id() + ":table_removed_refund", participant.bet, participant.bet,
                        "round=" + session.roundId);
            }
        }
        session.publicMessage = message;
    }

    private void pruneViewers(MinecraftServer server, Session session, CasinoMachineBlockEntity machine) {
        ServerWorld world = server.getWorld(session.key.world);
        if (world == null) return;
        BlockPos pos = machine.getPos();
        session.viewers.removeIf(id -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            return player == null || player.getWorld() != world
                    || player.squaredDistanceTo(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D) > 100.0D;
        });
    }

    private void broadcast(MinecraftServer server, CasinoMachineBlockEntity machine, Session session, String message) {
        if (server == null) return;
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>(session.viewers);
        recipients.addAll(session.participants.keySet());
        for (UUID id : recipients) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player == null || player.getWorld().getRegistryKey() != session.key.world) continue;
            if (player.squaredDistanceTo(machine.getPos().getX() + .5D, machine.getPos().getY() + .5D,
                    machine.getPos().getZ() + .5D) > 100.0D) continue;
            CasinoNetworking.sendTable(player, machine, stateFor(player, session, message));
        }
    }

    private CasinoNetworking.CasinoState stateFor(ServerPlayerEntity viewer, Session session, String message) {
        EmipokemonConfig.CasinoSettings settings = config();
        List<String> players = session.participants.values().stream()
                .map(p -> p.name + " · " + p.bet)
                .toList();
        String publicState = publicState(session);
        Participant mine = session.participants.get(viewer.getUuid());
        String privateState = privateState(session, mine);
        String effectiveMessage = mine != null && mine.lastMessage != null && !mine.lastMessage.isBlank()
                ? mine.lastMessage : message;
        return new CasinoNetworking.CasinoState(session.type.id(), session.type.displayName(), session.key.pos,
                Emipokemon.progressionService().balance(viewer.getUuid()), settings.minimumBet, settings.maximumBet,
                effectiveMessage, phaseId(session.phase), session.roundId, session.deadline, players, publicState, privateState,
                List.copyOf(session.rouletteHistory));
    }

    private String publicState(Session session) {
        if (session.phase == Phase.IDLE) return "Mesa libre. Inicia una ronda cuando quieras.";
        if (session.phase == Phase.BETTING) return "Apuestas abiertas · " + session.participants.size() + " participante(s).";
        if (session.phase == Phase.BLACKJACK) {
            return "Dealer: " + (session.dealer.isEmpty() ? "?" : card(session.dealer.getFirst()) + " + ?")
                    + " · " + session.participants.size() + " jugador(es).";
        }
        if (session.phase.isPoker()) {
            int reveal = Math.min(session.revealCount, session.board.size());
            return "Mesa: " + cards(session.board.subList(0, reveal)) + " · bote " + pot(session) + ".";
        }
        return session.publicMessage;
    }

    private String privateState(Session session, Participant participant) {
        if (participant == null) return "Modo espectador: puedes unirte en la próxima fase de apuestas.";
        if (session.type == CasinoGameType.ROULETTE || session.type == CasinoGameType.DICE) {
            return "Tu apuesta: " + participant.action + " · " + participant.bet + " Michicoins.";
        }
        if (session.type == CasinoGameType.BLACKJACK && !participant.hand.isEmpty()) {
            return "Tu mano: " + cards(participant.hand) + " = " + blackjackValue(participant.hand)
                    + (participant.done ? " · cerrada" : " · elige Pedir o Plantarse");
        }
        if (session.type == CasinoGameType.POKER && !participant.hand.isEmpty()) {
            return "Tus cartas: " + cards(participant.hand) + (participant.folded ? " · retirado" : " · sigues en juego");
        }
        return "Entrada reservada: " + participant.bet + " Michicoins.";
    }

    private String intro(Session session) {
        return switch (session.type) {
            case ROULETTE -> "Ruleta multijugador: todas las apuestas de la ronda comparten un único número.";
            case DICE -> "Dados multijugador: toda la mesa comparte una única tirada.";
            case BLACKJACK -> "Blackjack de mesa: dealer común y mano individual para cada jugador.";
            case POKER -> "Póker comunitario: entrada única por ronda, cartas privadas, mesa común y bote compartido.";
            default -> "Mesa compartida.";
        };
    }

    private long pot(Session session) {
        long value = 0L;
        for (Participant participant : session.participants.values()) value = Math.addExact(value, participant.bet);
        return value;
    }

    private boolean allDone(Session session) {
        return session.participants.values().stream().allMatch(p -> p.done);
    }

    private int activePokerPlayers(Session session) {
        return (int) session.participants.values().stream().filter(p -> !p.folded).count();
    }

    private void credit(Participant participant, long payout, String game) {
        if (payout > 0L) Emipokemon.progressionService().refund(participant.id, payout,
                "casino:" + game + ":round_payout");
    }

    private long payout(long bet, long multiplier, double scale) {
        return payoutDecimal(bet, multiplier, scale);
    }

    private long payoutDecimal(long base, double multiplier, double scale) {
        if (base <= 0L || multiplier <= 0D || scale <= 0D) return 0L;
        double value = base * multiplier * scale;
        long rounded = value >= Long.MAX_VALUE ? config().maximumPayout : Math.max(0L, Math.round(value));
        return Math.min(config().maximumPayout, rounded);
    }

    private EmipokemonConfig.CasinoSettings config() {
        return Emipokemon.configManager().get().casino;
    }

    private boolean validRoulette(String action) {
        if (Set.of("red", "black", "even", "odd", "low", "high", "dozen1", "dozen2", "dozen3",
                "column1", "column2", "column3").contains(action)) return true;
        if (!action.startsWith("number:")) return false;
        try {
            int number = Integer.parseInt(action.substring(7));
            return number >= 0 && number <= 36;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean rouletteWin(String action, int number) {
        if (action.startsWith("number:")) return Integer.parseInt(action.substring(7)) == number;
        return switch (action) {
            case "red" -> number > 0 && RED.contains(number);
            case "black" -> number > 0 && !RED.contains(number);
            case "even" -> number > 0 && number % 2 == 0;
            case "odd" -> number % 2 == 1;
            case "low" -> number >= 1 && number <= 18;
            case "high" -> number >= 19;
            case "dozen1" -> number >= 1 && number <= 12;
            case "dozen2" -> number >= 13 && number <= 24;
            case "dozen3" -> number >= 25;
            case "column1" -> number > 0 && (number - 1) % 3 == 0;
            case "column2" -> number > 0 && (number - 2) % 3 == 0;
            case "column3" -> number > 0 && number % 3 == 0;
            default -> false;
        };
    }

    private List<Integer> shuffledDeck() {
        List<Integer> deck = new ArrayList<>(52);
        for (int card = 0; card < 52; card++) deck.add(card);
        Collections.shuffle(deck, RANDOM);
        return deck;
    }

    private int draw(Session session) {
        if (session.deck.isEmpty()) session.deck.addAll(shuffledDeck());
        return session.deck.removeLast();
    }

    private int blackjackValue(List<Integer> cards) {
        int total = 0;
        int aces = 0;
        for (int card : cards) {
            int rank = card % 13 + 2;
            if (rank == 14) { total += 11; aces++; }
            else total += Math.min(rank, 10);
        }
        while (total > 21 && aces-- > 0) total -= 10;
        return total;
    }

    private List<Integer> join(List<Integer> first, List<Integer> second) {
        List<Integer> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private PokerHand bestPoker(List<Integer> cards) {
        PokerHand best = new PokerHand(0L, 0, "Carta alta");
        for (int a = 0; a < cards.size() - 4; a++) for (int b = a + 1; b < cards.size() - 3; b++)
            for (int c = b + 1; c < cards.size() - 2; c++) for (int d = c + 1; d < cards.size() - 1; d++)
                for (int e = d + 1; e < cards.size(); e++) {
                    PokerHand hand = scoreFive(List.of(cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
                    if (hand.score > best.score) best = hand;
                }
        return best;
    }

    private PokerHand scoreFive(List<Integer> cards) {
        List<Integer> ranks = cards.stream().map(card -> card % 13 + 2).sorted(Comparator.reverseOrder()).toList();
        boolean flush = cards.stream().map(card -> card / 13).distinct().count() == 1;
        Set<Integer> unique = new HashSet<>(ranks);
        int straightHigh = 0;
        for (int high = 14; high >= 5; high--) {
            boolean straight = true;
            for (int delta = 0; delta < 5; delta++) if (!unique.contains(high - delta)) straight = false;
            if (straight) { straightHigh = high; break; }
        }
        if (straightHigh == 0 && unique.containsAll(Set.of(14, 2, 3, 4, 5))) straightHigh = 5;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int rank : ranks) counts.merge(rank, 1, Integer::sum);
        List<Integer> ordered = counts.keySet().stream()
                .sorted(Comparator.<Integer>comparingInt(counts::get).reversed().thenComparing(Comparator.reverseOrder())).toList();
        int category;
        String name;
        List<Integer> tie;
        if (flush && straightHigh > 0) { category = 8; name = straightHigh == 14 ? "Escalera real" : "Escalera de color"; tie = List.of(straightHigh); }
        else if (counts.containsValue(4)) { category = 7; name = "Póker"; tie = ordered; }
        else if (counts.containsValue(3) && counts.containsValue(2)) { category = 6; name = "Full house"; tie = ordered; }
        else if (flush) { category = 5; name = "Color"; tie = ranks; }
        else if (straightHigh > 0) { category = 4; name = "Escalera"; tie = List.of(straightHigh); }
        else if (counts.containsValue(3)) { category = 3; name = "Trío"; tie = ordered; }
        else if (counts.values().stream().filter(count -> count == 2).count() == 2) { category = 2; name = "Doble pareja"; tie = ordered; }
        else if (counts.containsValue(2)) { category = 1; name = "Pareja"; tie = ordered; }
        else { category = 0; name = "Carta alta"; tie = ranks; }
        long score = category;
        for (int rank : tie) score = score * 15L + rank;
        for (int index = tie.size(); index < 5; index++) score *= 15L;
        return new PokerHand(score, category, name);
    }

    private String cards(List<Integer> cards) {
        return cards.stream().map(this::card).reduce((a, b) -> a + " " + b).orElse("—");
    }

    private String card(int card) {
        int rank = card % 13 + 2;
        String rankText = switch (rank) { case 14 -> "A"; case 13 -> "K"; case 12 -> "Q"; case 11 -> "J"; default -> Integer.toString(rank); };
        return rankText + switch (card / 13) { case 0 -> "♠"; case 1 -> "♥"; case 2 -> "♦"; default -> "♣"; };
    }

    private String phaseId(Phase phase) {
        return switch (phase) {
            case IDLE -> "idle";
            case BETTING -> "betting";
            case BLACKJACK -> "blackjack";
            case POKER_FLOP -> "poker_flop";
            case POKER_TURN -> "poker_turn";
            case POKER_RIVER -> "poker_river";
            case RESULT -> "result";
        };
    }

    private void audit(UUID playerId, String game, long bet, long payout, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            String safe = detail == null ? "" : detail.replace('\t', ' ').replace('\n', ' ');
            String line = System.currentTimeMillis() + "\t" + playerId + "\t" + game + "\t" + bet + "\t" + payout + "\t" + safe + "\n";
            Files.writeString(auditFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not append shared casino audit", exception);
        }
    }

    private enum Phase {
        IDLE, BETTING, BLACKJACK, POKER_FLOP, POKER_TURN, POKER_RIVER, RESULT;
        boolean isPoker() { return this == POKER_FLOP || this == POKER_TURN || this == POKER_RIVER; }
    }

    private static final class Session {
        final TableKey key;
        final CasinoGameType type;
        final LinkedHashMap<UUID, Participant> participants = new LinkedHashMap<>();
        final LinkedHashSet<UUID> viewers = new LinkedHashSet<>();
        final List<Integer> deck = new ArrayList<>();
        final List<Integer> dealer = new ArrayList<>();
        final List<Integer> board = new ArrayList<>();
        final java.util.ArrayDeque<Integer> rouletteHistory = new java.util.ArrayDeque<>();
        Phase phase = Phase.IDLE;
        long roundId;
        long deadline;
        long lastTouched = System.currentTimeMillis();
        int revealCount;
        String publicMessage = "Mesa lista.";
        Session(TableKey key, CasinoGameType type) { this.key = key; this.type = type; }
    }

    private static final class Participant {
        final UUID id;
        final String name;
        final long bet;
        final String action;
        final List<Integer> hand = new ArrayList<>();
        boolean done;
        boolean folded;
        String lastMessage = "";
        PokerHand pokerHand;
        Participant(UUID id, String name, long bet, String action) {
            this.id = id; this.name = name; this.bet = bet; this.action = action;
        }
    }

    private record TableKey(RegistryKey<World> world, long pos) { }
    private record PokerHand(long score, int category, String name) { }
}
