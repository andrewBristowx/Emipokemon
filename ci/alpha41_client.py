from pathlib import Path

root=Path('.')

def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text)

write('src/client/java/com/emipokemon/client/casino/CasinoScreen.java', r'''package com.emipokemon.client.casino;

import com.emipokemon.casino.CasinoNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

final class CasinoScreen extends Screen {
    private static final Gson GSON = new Gson();

    private static final int BACKDROP = 0xAD000000;
    private static final int SHADOW = 0xB0000000;
    private static final int PANEL = 0xFF100B16;
    private static final int HEADER = 0xFF17101F;
    private static final int CARD = 0xFF1A1223;
    private static final int CARD_2 = 0xFF21172C;
    private static final int LINE = 0xFF493757;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFD3C5DC;
    private static final int DIM = 0xFFA996B3;
    private static final int GOLD = 0xFFFFD166;
    private static final int GREEN = 0xFF8FE3B0;

    private final Screen parent;
    private final CasinoNetworking.CasinoState state;
    private final String previousAmount;

    private TextFieldWidget amountField;
    private TextFieldWidget numberField;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int bodyTop;
    private int bodyBottom;
    private int leftCardX;
    private int leftCardW;
    private int rightCardX;
    private int rightCardW;

    CasinoScreen(Screen parent, String json, String previousAmount) {
        super(Text.literal("Casino Emipokemon"));
        this.parent = parent;
        this.state = GSON.fromJson(json, CasinoNetworking.CasinoState.class);
        this.previousAmount = previousAmount;
    }

    Screen parentScreen() { return parent; }
    String amountText() { return amountField == null ? previousAmount : amountField.getText(); }

    @Override
    protected void init() {
        panelW = Math.min(980, width - 24);
        panelH = Math.min(600, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        bodyTop = panelY + 92;
        bodyBottom = panelY + panelH - 86;
        leftCardX = panelX + 24;
        leftCardW = Math.max(220, Math.min(420, (panelW - 72) * 44 / 100));
        rightCardX = leftCardX + leftCardW + 20;
        rightCardW = panelX + panelW - 24 - rightCardX;

        int inputX = leftCardX + 16;
        int inputW = leftCardW - 32;
        int inputY = bodyTop + 62;

        amountField = new TextFieldWidget(textRenderer, inputX, inputY, inputW, 24, Text.literal("Cantidad"));
        String starting = previousAmount == null || previousAmount.isBlank()
                ? Long.toString(Math.max(1L, state.minimumBet())) : previousAmount;
        amountField.setText(starting);
        amountField.setMaxLength(18);
        addDrawableChild(amountField);

        int quickY = inputY + 32;
        int quickGap = 6;
        int quickW = (inputW - quickGap * 2) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("Mínimo"), ignored -> setQuickAmount(1))
                .dimensions(inputX, quickY, quickW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x5"), ignored -> setQuickAmount(5))
                .dimensions(inputX + quickW + quickGap, quickY, quickW, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x10"), ignored -> setQuickAmount(10))
                .dimensions(inputX + (quickW + quickGap) * 2, quickY, quickW, 22).build());

        int actionsY = quickY + 38;
        List<Action> actions = actions();
        int actionGap = 10;
        int buttonW = (inputW - actionGap) / 2;
        for (int index = 0; index < actions.size(); index++) {
            Action action = actions.get(index);
            int col = index % 2;
            int row = index / 2;
            int x = inputX + col * (buttonW + actionGap);
            int y = actionsY + row * 31;
            addDrawableChild(ButtonWidget.builder(Text.literal(action.label), ignored -> send(action.action))
                    .dimensions(x, y, buttonW, 24).build());
        }

        if ("roulette".equals(state.game()) && isBettingPhase()) {
            int row = (actions.size() + 1) / 2;
            int y = actionsY + row * 31;
            numberField = new TextFieldWidget(textRenderer, inputX, y, 78, 24, Text.literal("0-36"));
            numberField.setText("0");
            numberField.setMaxLength(2);
            addDrawableChild(numberField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Apostar al número"), ignored -> sendExactNumber())
                    .dimensions(inputX + 88, y, inputW - 88, 24).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), ignored -> close())
                .dimensions(panelX + panelW - 118, panelY + 22, 92, 22).build());
    }

    private void setQuickAmount(int multiplier) {
        if (amountField == null) return;
        long base = Math.max(1L, state.minimumBet());
        long amount = base > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : base * multiplier;
        amountField.setText(Long.toString(amount));
    }

    private boolean isBettingPhase() {
        return "idle".equals(state.phase()) || "betting".equals(state.phase());
    }

    private boolean isSharedGame() {
        return switch (state.game() == null ? "" : state.game()) {
            case "roulette", "dice", "blackjack", "poker" -> true;
            default -> false;
        };
    }

    private List<Action> actions() {
        List<Action> result = new ArrayList<>();
        String game = state.game() == null ? "" : state.game();
        String phase = state.phase() == null ? "single" : state.phase();
        if ("slot".equals(game)) {
            result.add(new Action("Girar", "spin"));
        } else if ("chip_exchange".equals(game)) {
            result.add(new Action("Comprar fichas", "buy"));
            result.add(new Action("Canjear fichas", "sell"));
        } else if ("ticket_exchange".equals(game)) {
            result.add(new Action("Comprar ticket", "buy_ticket"));
        } else if ("roulette".equals(game) && isBettingPhase()) {
            result.add(new Action("Rojo", "red"));
            result.add(new Action("Negro", "black"));
            result.add(new Action("Par", "even"));
            result.add(new Action("Impar", "odd"));
            result.add(new Action("1–18", "low"));
            result.add(new Action("19–36", "high"));
            result.add(new Action("1ª docena", "dozen1"));
            result.add(new Action("2ª docena", "dozen2"));
            result.add(new Action("3ª docena", "dozen3"));
        } else if ("dice".equals(game) && isBettingPhase()) {
            result.add(new Action("Menos de 7", "under7"));
            result.add(new Action("Más de 7", "over7"));
            result.add(new Action("Exactamente 7", "exact7"));
        } else if ("blackjack".equals(game)) {
            if (isBettingPhase()) {
                result.add(new Action("Unirse a la mano", "join"));
            } else if ("blackjack".equals(phase)) {
                result.add(new Action("Pedir", "hit"));
                result.add(new Action("Plantarse", "stand"));
            }
        } else if ("poker".equals(game)) {
            if (isBettingPhase()) {
                result.add(new Action("Entrar al bote", "join"));
            } else if (phase.startsWith("poker_")) {
                result.add(new Action("Retirarse", "fold"));
            }
        }
        return result;
    }

    private void sendExactNumber() {
        String value = numberField == null ? "" : numberField.getText().strip();
        try {
            int number = Integer.parseInt(value);
            if (number < 0 || number > 36) return;
            send("number:" + number);
        } catch (NumberFormatException ignored) { }
    }

    private void send(String action) {
        long amount;
        try {
            amount = Long.parseLong(amountField.getText().strip());
        } catch (Exception exception) {
            amount = 0L;
        }
        ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload(state.blockPos(), action, amount));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKDROP);

        int accent = gameAccent();
        context.fill(panelX - 4, panelY + 4, panelX + panelW + 4, panelY + panelH + 6, SHADOW);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        context.fill(panelX, panelY, panelX + panelW, panelY + 6, accent);
        context.fill(panelX, panelY + 6, panelX + panelW, panelY + 78, HEADER);
        context.fill(panelX + 24, panelY + 78, panelX + panelW - 24, panelY + 79, LINE);

        String title = safe(state.title(), "Casino Emipokemon");
        context.drawTextWithShadow(textRenderer, Text.literal(title), panelX + 26, panelY + 23, WHITE);
        String mode = isSharedGame() ? "MESA MULTIJUGADOR · servidor autoritativo" : "SERVICIO PERSONAL";
        context.drawTextWithShadow(textRenderer, Text.literal(mode), panelX + 26, panelY + 47, isSharedGame() ? GREEN : MUTED);

        String balance = "Saldo: " + state.balance() + " Michicoins";
        int balanceX = panelX + panelW - 132 - textRenderer.getWidth(balance);
        drawBadge(context, balanceX, panelY + 50, balance, GOLD, 0xFF2A1F22);

        drawCard(context, leftCardX, bodyTop, leftCardW, bodyBottom - bodyTop, accent);
        drawCard(context, rightCardX, bodyTop, rightCardW, bodyBottom - bodyTop, accent);

        context.drawTextWithShadow(textRenderer, Text.literal("APUESTA / ACCIÓN"), leftCardX + 16, bodyTop + 15, accent);
        String hint = gameHint();
        drawWrapped(context, hint, leftCardX + 16, bodyTop + 33, leftCardW - 32, DIM, bodyTop + 58);
        context.drawTextWithShadow(textRenderer, Text.literal("Cantidad"), leftCardX + 16, bodyTop + 50, MUTED);

        context.drawTextWithShadow(textRenderer, Text.literal("ESTADO DE LA MESA"), rightCardX + 16, bodyTop + 15, accent);
        String phase = phaseLabel();
        String timer = timerLabel();
        drawBadge(context, rightCardX + 16, bodyTop + 34, phase + timer, GREEN, CARD_2);

        int y = bodyTop + 67;
        context.drawTextWithShadow(textRenderer, Text.literal("Mesa"), rightCardX + 16, y, MUTED);
        y = drawWrapped(context, safe(state.tableState(), "Sin datos de mesa"), rightCardX + 16, y + 16,
                rightCardW - 32, WHITE, bodyTop + 151);

        y = Math.max(y + 8, bodyTop + 159);
        context.fill(rightCardX + 16, y, rightCardX + rightCardW - 16, y + 1, LINE);
        y += 11;
        context.drawTextWithShadow(textRenderer, Text.literal("Tu estado"), rightCardX + 16, y, GOLD);
        y = drawWrapped(context, safe(state.privateState(), "Sin acción privada pendiente"), rightCardX + 16, y + 16,
                rightCardW - 32, WHITE, bodyTop + 245);

        y = Math.max(y + 8, bodyTop + 252);
        context.fill(rightCardX + 16, y, rightCardX + rightCardW - 16, y + 1, LINE);
        y += 11;
        List<String> players = state.players() == null ? List.of() : state.players();
        String playersTitle = isSharedGame() ? "Jugadores / apuestas · " + players.size() : "Actividad";
        context.drawTextWithShadow(textRenderer, Text.literal(playersTitle), rightCardX + 16, y, MUTED);
        y += 17;
        if (players.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(isSharedGame() ? "Mesa libre: esperando jugadores" : "Sin actividad compartida"),
                    rightCardX + 16, y, DIM);
        } else {
            int maxY = bodyBottom - 18;
            for (String player : players) {
                if (y > maxY) break;
                context.fill(rightCardX + 16, y + 2, rightCardX + 19, y + 11, accent);
                context.drawTextWithShadow(textRenderer, Text.literal(player), rightCardX + 25, y, WHITE);
                y += 14;
            }
        }

        int messageTop = panelY + panelH - 70;
        context.fill(panelX + 24, messageTop, panelX + panelW - 24, panelY + panelH - 22, CARD_2);
        outline(context, panelX + 24, messageTop, panelW - 48, 48, LINE);
        context.fill(panelX + 24, messageTop, panelX + 29, panelY + panelH - 22, accent);
        context.drawTextWithShadow(textRenderer, Text.literal("CASINO"), panelX + 40, messageTop + 8, accent);
        drawWrapped(context, safe(state.message(), "Listo para jugar"), panelX + 40, messageTop + 23,
                panelW - 82, WHITE, panelY + panelH - 27);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCard(DrawContext context, int x, int y, int w, int h, int accent) {
        context.fill(x, y, x + w, y + h, CARD);
        outline(context, x, y, w, h, LINE);
        context.fill(x, y, x + 4, y + h, accent);
    }

    private void drawBadge(DrawContext context, int x, int y, String text, int color, int background) {
        int w = textRenderer.getWidth(text) + 14;
        context.fill(x, y, x + w, y + 18, background);
        outline(context, x, y, w, 18, LINE);
        context.drawTextWithShadow(textRenderer, Text.literal(text), x + 7, y + 5, color);
    }

    private void outline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private int drawWrapped(DrawContext context, String value, int x, int y, int width, int color, int maxY) {
        if (value == null || value.isBlank()) return y;
        for (var line : textRenderer.wrapLines(Text.literal(value), Math.max(40, width))) {
            if (y > maxY) break;
            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 13;
        }
        return y;
    }

    private String gameHint() {
        return switch (state.game() == null ? "" : state.game()) {
            case "slot" -> "Elige la cantidad y gira los rodillos.";
            case "chip_exchange" -> "Compra o canjea fichas con tu saldo.";
            case "ticket_exchange" -> "Convierte Michicoins en tickets del casino.";
            case "roulette" -> "Apuesta mientras la mesa esté abierta.";
            case "dice" -> "Comparte la ronda y apuesta al total de los dados.";
            case "blackjack" -> "Entra en la mano y juega contra la banca.";
            case "poker" -> "Entra al bote y compite en la mesa compartida.";
            default -> "Selecciona una acción del casino.";
        };
    }

    private int gameAccent() {
        return switch (state.game() == null ? "" : state.game()) {
            case "slot" -> 0xFFE5A73A;
            case "chip_exchange" -> 0xFF52C7B8;
            case "ticket_exchange" -> 0xFFE680A2;
            case "roulette" -> 0xFFE0525B;
            case "poker" -> 0xFF69C487;
            case "blackjack" -> 0xFF77A7E8;
            case "dice" -> 0xFFC38ADF;
            default -> 0xFFFFC857;
        };
    }

    private String phaseLabel() {
        return switch (state.phase() == null ? "single" : state.phase()) {
            case "idle" -> "Mesa libre";
            case "betting" -> "Apuestas abiertas";
            case "blackjack" -> "Blackjack en juego";
            case "poker_flop" -> "Póker · Flop";
            case "poker_turn" -> "Póker · Turn";
            case "poker_river" -> "Póker · River";
            case "result" -> "Resultado";
            default -> "Operación individual";
        };
    }

    private String timerLabel() {
        if (state.deadlineMillis() <= 0L) return "";
        long left = Math.max(0L, (state.deadlineMillis() - System.currentTimeMillis() + 999L) / 1000L);
        return " · " + left + " s";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty: the casino draws its own deterministic dim overlay and must not
        // invoke Minecraft/Cobbleverse post-process blur after widgets have been rendered.
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private record Action(String label, String action) { }
}
''')
