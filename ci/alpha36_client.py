from pathlib import Path

root=Path('.')

def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text)

write('src/client/java/com/emipokemon/client/casino/CasinoClient.java', r'''package com.emipokemon.client.casino;

import com.emipokemon.casino.CasinoNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class CasinoClient {
    private static boolean initialized;

    private CasinoClient() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(CasinoNetworking.OpenCasinoPayload.ID, (payload, context) ->
                context.client().execute(() -> open(payload.json())));
    }

    private static void open(String json) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen current = client.currentScreen;
        Screen parent = current;
        String previousAmount = null;
        if (current instanceof CasinoScreen casino) {
            parent = casino.parentScreen();
            previousAmount = casino.amountText();
        }
        client.setScreen(new CasinoScreen(parent, json, previousAmount));
    }
}
''')

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
    private static final int PANEL = 0xF2140819;
    private static final int PANEL_2 = 0xE91F1028;
    private static final int GOLD = 0xFFFFC857;
    private static final int PINK = 0xFFFF63B7;
    private static final int MUTED = 0xFFBDAFC5;
    private static final int WHITE = 0xFFF8F2FF;
    private static final int GREEN = 0xFF8DE6B5;

    private final Screen parent;
    private final CasinoNetworking.CasinoState state;
    private final String previousAmount;
    private TextFieldWidget amountField;
    private TextFieldWidget numberField;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

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
        panelW = Math.min(940, width - 28);
        panelH = Math.min(540, height - 28);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int leftX = panelX + 28;
        int controlsW = Math.min(430, (panelW - 84) / 2);
        int inputY = panelY + 116;
        amountField = new TextFieldWidget(textRenderer, leftX, inputY, 258, 24, Text.literal("Cantidad"));
        String starting = previousAmount == null || previousAmount.isBlank()
                ? Long.toString(Math.max(1L, state.minimumBet())) : previousAmount;
        amountField.setText(starting);
        amountField.setMaxLength(18);
        addDrawableChild(amountField);

        int actionsY = inputY + 42;
        List<Action> actions = actions();
        int buttonW = Math.max(118, (controlsW - 12) / 2);
        int index = 0;
        for (Action action : actions) {
            int col = index % 2;
            int row = index / 2;
            int x = leftX + col * (buttonW + 12);
            int y = actionsY + row * 31;
            ButtonWidget button = ButtonWidget.builder(Text.literal(action.label), ignored -> send(action.action))
                    .dimensions(x, y, buttonW, 24).build();
            addDrawableChild(button);
            index++;
        }

        if ("roulette".equals(state.game()) && ("idle".equals(state.phase()) || "betting".equals(state.phase()))) {
            int row = (actions.size() + 1) / 2;
            int y = actionsY + row * 31;
            numberField = new TextFieldWidget(textRenderer, leftX, y, 92, 24, Text.literal("0-36"));
            numberField.setText("0");
            numberField.setMaxLength(2);
            addDrawableChild(numberField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Número exacto"), ignored -> sendExactNumber())
                    .dimensions(leftX + 104, y, buttonW * 2 - 104 + 12, 24).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), ignored -> close())
                .dimensions(panelX + panelW - 146, panelY + panelH - 48, 118, 24).build());
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
            result.add(new Action("Comprar ticket normal", "buy_ticket"));
        } else if ("roulette".equals(game) && ("idle".equals(phase) || "betting".equals(phase))) {
            result.add(new Action("Rojo", "red")); result.add(new Action("Negro", "black"));
            result.add(new Action("Par", "even")); result.add(new Action("Impar", "odd"));
            result.add(new Action("1–18", "low")); result.add(new Action("19–36", "high"));
            result.add(new Action("1ª docena", "dozen1")); result.add(new Action("2ª docena", "dozen2"));
            result.add(new Action("3ª docena", "dozen3"));
        } else if ("dice".equals(game) && ("idle".equals(phase) || "betting".equals(phase))) {
            result.add(new Action("Menos de 7", "under7"));
            result.add(new Action("Más de 7", "over7"));
            result.add(new Action("Exactamente 7", "exact7"));
        } else if ("blackjack".equals(game)) {
            if ("idle".equals(phase) || "betting".equals(phase)) {
                result.add(new Action("Unirse a la mano", "join"));
            } else if ("blackjack".equals(phase)) {
                result.add(new Action("Pedir", "hit"));
                result.add(new Action("Plantarse", "stand"));
            }
        } else if ("poker".equals(game)) {
            if ("idle".equals(phase) || "betting".equals(phase)) {
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
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        context.fill(panelX, panelY, panelX + panelW, panelY + 5, GOLD);
        context.fill(panelX, panelY + 5, panelX + panelW, panelY + 9, PINK);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(state.title()), panelX + panelW / 2, panelY + 24, WHITE);
        context.drawTextWithShadow(textRenderer, Text.literal("Saldo: " + state.balance() + " Michicoins"), panelX + 28, panelY + 57, GOLD);
        String phase = phaseLabel();
        String timer = timerLabel();
        context.drawTextWithShadow(textRenderer, Text.literal(phase + timer), panelX + panelW - 28 - textRenderer.getWidth(phase + timer), panelY + 57, GREEN);

        int dividerX = panelX + panelW / 2;
        context.fill(dividerX, panelY + 84, dividerX + 1, panelY + panelH - 72, 0x558B6F97);
        context.fill(panelX + 20, panelY + 84, panelX + panelW - 20, panelY + 85, 0x448B6F97);

        context.drawTextWithShadow(textRenderer, Text.literal("Cantidad"), panelX + 28, panelY + 96, MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal("Estado de la mesa"), dividerX + 24, panelY + 96, PINK);
        int y = drawWrapped(context, state.tableState(), dividerX + 24, panelY + 116, panelW / 2 - 52, WHITE);
        y += 10;
        context.drawTextWithShadow(textRenderer, Text.literal("Tu estado"), dividerX + 24, y, GOLD);
        y = drawWrapped(context, state.privateState(), dividerX + 24, y + 18, panelW / 2 - 52, WHITE);
        y += 12;
        context.drawTextWithShadow(textRenderer, Text.literal("Jugadores / apuestas"), dividerX + 24, y, PINK);
        y += 18;
        List<String> players = state.players() == null ? List.of() : state.players();
        if (players.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("— mesa vacía —"), dividerX + 24, y, MUTED);
        } else {
            for (String player : players) {
                context.drawTextWithShadow(textRenderer, Text.literal("• " + player), dividerX + 24, y, WHITE);
                y += 14;
                if (y > panelY + panelH - 135) break;
            }
        }

        int messageTop = panelY + panelH - 96;
        context.fill(panelX + 20, messageTop, panelX + panelW - 20, panelY + panelH - 58, PANEL_2);
        drawWrapped(context, state.message(), panelX + 32, messageTop + 13, panelW - 210, WHITE);
        super.render(context, mouseX, mouseY, delta);
    }

    private int drawWrapped(DrawContext context, String value, int x, int y, int width, int color) {
        if (value == null || value.isBlank()) return y;
        for (var line : textRenderer.wrapLines(Text.literal(value), width)) {
            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 13;
        }
        return y;
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

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private record Action(String label, String action) { }
}
''')
