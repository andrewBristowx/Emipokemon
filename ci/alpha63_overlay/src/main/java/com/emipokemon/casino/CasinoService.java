package com.emipokemon.casino;

import com.emipokemon.Emipokemon;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.registry.ModRegistries;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CasinoService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<Integer> RED = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final String[] SLOT_SYMBOLS = {"Cereza", "Baya", "Campana", "Estrella", "Emi", "Jackpot"};
    private static final Map<UUID, Long> LAST_ACTION = new ConcurrentHashMap<>();
    private final Path auditFile = Emipokemon.configManager().configDirectory().resolve("casino-audit.log");

    public synchronized Result play(ServerPlayerEntity player, CasinoMachineBlockEntity machine,
                                    String rawAction, long amount) {
        EmipokemonConfig.CasinoSettings config = Emipokemon.configManager().get().casino;
        if (!config.enabled) return result(player, "El casino está desactivado por configuración.");
        long now = System.currentTimeMillis();
        if (now - LAST_ACTION.getOrDefault(player.getUuid(), 0L) < 450L) {
            return result(player, "Espera un momento antes de volver a jugar.");
        }
        LAST_ACTION.put(player.getUuid(), now);
        String action = rawAction == null ? "" : rawAction.strip().toLowerCase(java.util.Locale.ROOT);
        try {
            Result result = switch (machine.gameType()) {
                case CHIP_EXCHANGE -> exchangeChips(player, action, amount, config);
                case TICKET_EXCHANGE -> buyTickets(player, action, amount, config);
                case SLOT -> slot(player, amount, config);
                case CLAW -> result(player, "La máquina de garra usa su sesión interactiva autoritativa.");
                case ROULETTE, DICE, BLACKJACK, POKER ->
                        result(player, "Esta mesa se resuelve mediante la sesión multijugador compartida.");
                case POKEMON_FLIP -> result(player, "Esta mesa se resuelve mediante el duelo Pokémon.");
            };
            machine.activate();
            return result;
        } catch (ArithmeticException exception) {
            return result(player, "La cantidad supera el límite seguro.");
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Casino action failed for {}", player.getUuid(), exception);
            return result(player, "La jugada fue cancelada de forma segura.");
        }
    }

    private Result exchangeChips(ServerPlayerEntity player, String action, long quantity,
                                 EmipokemonConfig.CasinoSettings config) {
        int count = safeQuantity(quantity);
        long value = Math.multiplyExact(config.chipPrice, count);
        if ("sell".equals(action)) {
            if (!removeItems(player, ModRegistries.CASINO_CHIP, count)) return result(player, "No tienes suficientes fichas.");
            Emipokemon.progressionService().refund(player, value, "casino:chips:sell");
            audit(player, "chips_sell", value, 0L, "cantidad=" + count);
            return result(player, "Canjeaste " + count + " fichas por " + value + " Michicoins.");
        }
        if (!"buy".equals(action)) return result(player, "Acción de canje inválida.");
        if (!Emipokemon.progressionService().spend(player, value, "casino:chips:buy")) {
            return result(player, "No tienes suficientes Michicoins.");
        }
        give(player, new ItemStack(ModRegistries.CASINO_CHIP, count));
        audit(player, "chips_buy", value, 0L, "cantidad=" + count);
        return result(player, "Compraste " + count + " fichas por " + value + " Michicoins.");
    }

    private Result buyTickets(ServerPlayerEntity player, String action, long quantity,
                              EmipokemonConfig.CasinoSettings config) {
        if (!Set.of("buy", "buy_ticket").contains(action)) return result(player, "La máquina solo entrega tickets de garra.");
        int count = safeQuantity(quantity);
        long price = Math.multiplyExact(config.clawTicketPrice, count);
        if (!Emipokemon.progressionService().spend(player, price, "casino:claw_ticket")) {
            return result(player, "No tienes suficientes Michicoins.");
        }
        give(player, new ItemStack(ModRegistries.CLAW_TICKET, count));
        audit(player, "claw_ticket", price, 0L, "cantidad=" + count);
        return result(player, "Recibiste " + count + " ticket(s) de garra. El ticket gacha no sirve en la garra.");
    }

    private Result slot(ServerPlayerEntity player, long bet, EmipokemonConfig.CasinoSettings config) {
        if (!reserveBet(player, bet, "slot", config)) return result(player, betError(bet, config));
        int a = slotSymbol();
        int b = slotSymbol();
        int c = slotSymbol();
        long multiplier = a == b && b == c ? new long[]{3, 4, 6, 10, 25, 100}[a]
                : (a == b || a == c || b == c ? 2L : 0L);
        long payout = payout(bet, multiplier, config.slotPayoutMultiplier, config);
        pay(player, payout, "slot");
        String detail = SLOT_SYMBOLS[a] + " | " + SLOT_SYMBOLS[b] + " | " + SLOT_SYMBOLS[c];
        audit(player, "slot", bet, payout, detail);
        return result(player, detail + (payout > 0 ? " — premio " + payout : " — sin premio"));
    }

    private Result roulette(ServerPlayerEntity player, String action, long bet,
                            EmipokemonConfig.CasinoSettings config) {
        if (!validRouletteAction(action)) return result(player, "Apuesta de ruleta inválida.");
        if (!reserveBet(player, bet, "roulette:" + action, config)) return result(player, betError(bet, config));
        int number = RANDOM.nextInt(37);
        boolean win;
        long multiplier;
        if (action.startsWith("number:")) {
            int selected = Integer.parseInt(action.substring(7));
            win = selected == number;
            multiplier = 36L;
        } else {
            win = switch (action) {
                case "red" -> number > 0 && RED.contains(number);
                case "black" -> number > 0 && !RED.contains(number);
                case "even" -> number > 0 && number % 2 == 0;
                case "odd" -> number % 2 == 1;
                case "low" -> number >= 1 && number <= 18;
                case "high" -> number >= 19;
                case "dozen1" -> number >= 1 && number <= 12;
                case "dozen2" -> number >= 13 && number <= 24;
                case "dozen3" -> number >= 25;
                default -> false;
            };
            multiplier = action.startsWith("dozen") ? 3L : 2L;
        }
        long payout = win ? payout(bet, multiplier, config.roulettePayoutMultiplier, config) : 0L;
        pay(player, payout, "roulette:" + action);
        String color = number == 0 ? "verde" : (RED.contains(number) ? "rojo" : "negro");
        audit(player, "roulette:" + action, bet, payout, number + ":" + color);
        return result(player, "Salió " + number + " " + color + (win ? " — premio " + payout : " — perdiste"));
    }

    private Result dice(ServerPlayerEntity player, String action, long bet,
                        EmipokemonConfig.CasinoSettings config) {
        if (!Set.of("under7", "over7", "exact7").contains(action)) return result(player, "Apuesta de dados inválida.");
        if (!reserveBet(player, bet, "dice:" + action, config)) return result(player, betError(bet, config));
        int first = RANDOM.nextInt(6) + 1;
        int second = RANDOM.nextInt(6) + 1;
        int total = first + second;
        boolean win = switch (action) {
            case "under7" -> total < 7;
            case "over7" -> total > 7;
            default -> total == 7;
        };
        long payout = win ? payout(bet, "exact7".equals(action) ? 6L : 2L,
                config.dicePayoutMultiplier, config) : 0L;
        pay(player, payout, "dice:" + action);
        audit(player, "dice:" + action, bet, payout, first + "+" + second);
        return result(player, "Dados: " + first + " + " + second + " = " + total
                + (win ? " — premio " + payout : " — perdiste"));
    }

    private Result blackjack(ServerPlayerEntity player, long bet, EmipokemonConfig.CasinoSettings config) {
        if (!reserveBet(player, bet, "blackjack", config)) return result(player, betError(bet, config));
        List<Integer> deck = shuffledDeck();
        List<Integer> hand = new ArrayList<>(List.of(deck.removeLast(), deck.removeLast()));
        List<Integer> dealer = new ArrayList<>(List.of(deck.removeLast(), deck.removeLast()));
        while (blackjackValue(hand) < 17) hand.add(deck.removeLast());
        while (blackjackValue(dealer) < 17) dealer.add(deck.removeLast());
        int playerValue = blackjackValue(hand);
        int dealerValue = blackjackValue(dealer);
        boolean natural = hand.size() == 2 && playerValue == 21;
        boolean win = playerValue <= 21 && (dealerValue > 21 || playerValue > dealerValue);
        boolean tie = playerValue <= 21 && playerValue == dealerValue;
        long payout = tie ? payout(bet, 1L, config.blackjackPayoutMultiplier, config)
                : win ? payout(bet, natural ? 5L : 2L, natural ? config.blackjackPayoutMultiplier / 2.0D
                : config.blackjackPayoutMultiplier, config) : 0L;
        pay(player, payout, "blackjack");
        String detail = "Tú " + playerValue + " vs casa " + dealerValue;
        audit(player, "blackjack", bet, payout, detail);
        return result(player, detail + (tie ? " — empate" : win ? " — premio " + payout : " — perdiste"));
    }

    private Result poker(ServerPlayerEntity player, String action, long bet, EmipokemonConfig.CasinoSettings config) {
        if (!Set.of("house", "pair_plus", "jackpot").contains(action)) return result(player, "Apuesta de póker inválida.");
        if (!reserveBet(player, bet, "poker:" + action, config)) return result(player, betError(bet, config));
        List<Integer> deck = shuffledDeck();
        List<Integer> playerCards = List.of(deck.removeLast(), deck.removeLast());
        List<Integer> houseCards = List.of(deck.removeLast(), deck.removeLast());
        List<Integer> board = List.of(deck.removeLast(), deck.removeLast(), deck.removeLast(), deck.removeLast(), deck.removeLast());
        PokerHand playerHand = bestPoker(join(playerCards, board));
        PokerHand houseHand = bestPoker(join(houseCards, board));
        long multiplier = 0L;
        String outcome;
        if ("house".equals(action)) {
            int comparison = Long.compare(playerHand.score(), houseHand.score());
            multiplier = comparison > 0 ? 2L : comparison == 0 ? 1L : 0L;
            outcome = comparison > 0 ? "ganaste a la casa" : comparison == 0 ? "empate" : "ganó la casa";
        } else if ("pair_plus".equals(action)) {
            multiplier = switch (playerHand.category()) {
                case 1 -> 2L; case 2 -> 3L; case 3 -> 5L; case 4 -> 8L; case 5 -> 10L;
                case 6 -> 15L; case 7 -> 50L; case 8 -> 100L; default -> 0L;
            };
            outcome = multiplier > 0 ? "premio por mano" : "se requería al menos una pareja";
        } else {
            multiplier = switch (playerHand.category()) {
                case 6 -> 10L; case 7 -> 40L; case 8 -> 120L; default -> 0L;
            };
            outcome = multiplier > 0 ? "jackpot" : "se requería full house o mejor";
        }
        long payout = payout(bet, multiplier, config.pokerPayoutMultiplier, config);
        pay(player, payout, "poker:" + action);
        String detail = "Tus cartas " + cards(playerCards) + " · mesa " + cards(board) + " · " + playerHand.name();
        audit(player, "poker:" + action, bet, payout, detail);
        return result(player, detail + " — " + outcome + (payout > 0 ? " (" + payout + ")" : ""));
    }

    private boolean reserveBet(ServerPlayerEntity player, long bet, String game,
                               EmipokemonConfig.CasinoSettings config) {
        return bet >= config.minimumBet && bet <= config.maximumBet
                && Emipokemon.progressionService().spend(player, bet, "casino:" + game + ":bet_reserved");
    }

    private String betError(long bet, EmipokemonConfig.CasinoSettings config) {
        return bet < config.minimumBet || bet > config.maximumBet
                ? "La apuesta debe estar entre " + config.minimumBet + " y " + config.maximumBet + "."
                : "No tienes suficientes Michicoins.";
    }

    private long payout(long bet, long multiplier, double scale, EmipokemonConfig.CasinoSettings config) {
        if (bet <= 0L || multiplier <= 0L || scale <= 0D) return 0L;
        double value = bet * (double) multiplier * scale;
        return Math.min(config.maximumPayout, value >= Long.MAX_VALUE ? config.maximumPayout : Math.max(0L, Math.round(value)));
    }

    private void pay(ServerPlayerEntity player, long payout, String game) {
        if (payout > 0L) Emipokemon.progressionService().refund(player, payout, "casino:" + game + ":payout");
    }

    private Result result(ServerPlayerEntity player, String message) {
        return new Result(message, Emipokemon.progressionService().balance(player.getUuid()));
    }

    private int safeQuantity(long quantity) {
        if (quantity < 1L || quantity > 64L) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 64.");
        return (int) quantity;
    }

    private boolean removeItems(ServerPlayerEntity player, Item item, int amount) {
        if (player.getInventory().count(item) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().size() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(item)) continue;
            int removed = Math.min(stack.getCount(), remaining);
            stack.decrement(removed);
            remaining -= removed;
        }
        player.getInventory().markDirty();
        return remaining == 0;
    }

    private void give(ServerPlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) player.dropItem(stack, false);
    }

    private int slotSymbol() {
        int roll = RANDOM.nextInt(100);
        if (roll < 30) return 0;
        if (roll < 55) return 1;
        if (roll < 75) return 2;
        if (roll < 90) return 3;
        if (roll < 98) return 4;
        return 5;
    }

    private boolean validRouletteAction(String action) {
        if (Set.of("red", "black", "even", "odd", "low", "high", "dozen1", "dozen2", "dozen3").contains(action)) return true;
        if (!action.startsWith("number:")) return false;
        try {
            int number = Integer.parseInt(action.substring(7));
            return number >= 0 && number <= 36;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private List<Integer> shuffledDeck() {
        List<Integer> deck = new ArrayList<>(52);
        for (int card = 0; card < 52; card++) deck.add(card);
        java.util.Collections.shuffle(deck, RANDOM);
        return deck;
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
                    if (hand.score() > best.score()) best = hand;
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
                .sorted(Comparator.<Integer>comparingInt(counts::get).reversed().thenComparing(Comparator.reverseOrder()))
                .toList();
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
        return cards.stream().map(this::card).reduce((left, right) -> left + " " + right).orElse("");
    }

    private String card(int card) {
        int rank = card % 13 + 2;
        String rankText = switch (rank) { case 14 -> "A"; case 13 -> "K"; case 12 -> "Q"; case 11 -> "J"; default -> Integer.toString(rank); };
        return rankText + switch (card / 13) { case 0 -> "♠"; case 1 -> "♥"; case 2 -> "♦"; default -> "♣"; };
    }

    private void audit(ServerPlayerEntity player, String game, long bet, long payout, String detail) {
        try {
            Files.createDirectories(auditFile.getParent());
            String safeDetail = detail.replace('\t', ' ').replace('\n', ' ');
            String line = System.currentTimeMillis() + "\t" + player.getUuid() + "\t" + game + "\t"
                    + bet + "\t" + payout + "\t" + safeDetail + "\n";
            Files.writeString(auditFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not append casino audit", exception);
        }
    }

    public record Result(String message, long balance) {
    }

    private record PokerHand(long score, int category, String name) {
    }
}
