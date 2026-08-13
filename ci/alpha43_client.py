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
import java.util.Set;

final class CasinoScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final Set<Integer> RED_NUMBERS = Set.of(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36);
    private static final int[] ROULETTE_WHEEL = {
            0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26
    };
    private static final double TAU = Math.PI * 2.0D;

    private static final int BACKDROP = 0xD20A070B;
    private static final int PANEL = 0xFF17100F;
    private static final int PANEL_2 = 0xFF211715;
    private static final int FELT = 0xFF17472F;
    private static final int FELT_2 = 0xFF1C5A3A;
    private static final int GOLD = 0xFFFFD166;
    private static final int GOLD_DARK = 0xFF9A6E2A;
    private static final int RED = 0xFFB93636;
    private static final int BLACK = 0xFF171717;
    private static final int GREEN = 0xFF1A6B45;
    private static final int WHITE = 0xFFF7F0E4;
    private static final int MUTED = 0xFFCABEA9;
    private static final int LINE = 0xFF6B523B;
    private static final int CHIP = 0xFFFFC94D;

    private final Screen parent;
    private final CasinoNetworking.CasinoState state;
    private final String previousAmount;
    private final long openedAt = System.currentTimeMillis();

    private TextFieldWidget amountField;
    private final List<RouletteCell> rouletteCells = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int gameX;
    private int gameW;
    private int sideX;
    private int sideW;
    private int contentTop;
    private int rouletteBoardY;
    private int rouletteCellW;
    private int rouletteCellH;
    private int wheelCx;
    private int wheelCy;
    private int wheelRadius;

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
        rouletteCells.clear();
        panelW = Math.min(1080, Math.max(520, width - 20));
        panelH = Math.min(660, Math.max(400, height - 20));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentTop = panelY + 68;

        addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), ignored -> close())
                .dimensions(panelX + panelW - 96, panelY + 22, 72, 20).build());

        if (isRoulette()) initRoulette();
        else initGeneric();
    }

    private void initRoulette() {
        sideW = Math.min(292, Math.max(246, panelW / 3));
        int gap = 14;
        gameX = panelX + 18;
        gameW = panelW - sideW - gap - 36;
        sideX = gameX + gameW + gap;

        int amountY = contentTop + 88;
        amountField = new TextFieldWidget(textRenderer, sideX + 14, amountY, sideW - 28, 22, Text.literal("Cantidad"));
        amountField.setText(startAmount());
        amountField.setMaxLength(18);
        addDrawableChild(amountField);

        int quickY = amountY + 30;
        int gapQ = 5;
        int qW = (sideW - 28 - gapQ * 2) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("Mín"), ignored -> setQuickAmount(1))
                .dimensions(sideX + 14, quickY, qW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x5"), ignored -> setQuickAmount(5))
                .dimensions(sideX + 14 + qW + gapQ, quickY, qW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x10"), ignored -> setQuickAmount(10))
                .dimensions(sideX + 14 + (qW + gapQ) * 2, quickY, qW, 20).build());

        wheelCx = gameX + gameW / 2;
        wheelCy = contentTop + 91;
        wheelRadius = Math.min(76, Math.max(56, gameW / 9));
        rouletteBoardY = contentTop + 188;
        buildRouletteCells();
    }

    private void buildRouletteCells() {
        int boardX = gameX + 8;
        rouletteCellH = 30;
        int zeroW = 42;
        int columnW = 42;
        int numberX = boardX + zeroW + 4;
        int available = gameW - 16 - zeroW - 4 - columnW - 4;
        rouletteCellW = Math.max(31, available / 12);
        int numberW = rouletteCellW * 12;

        rouletteCells.add(new RouletteCell(boardX, rouletteBoardY, zeroW, rouletteCellH * 3, "0", "number:0"));
        for (int column = 0; column < 12; column++) {
            int x = numberX + column * rouletteCellW;
            int base = (column + 1) * 3;
            rouletteCells.add(new RouletteCell(x, rouletteBoardY, rouletteCellW, rouletteCellH, Integer.toString(base), "number:" + base));
            rouletteCells.add(new RouletteCell(x, rouletteBoardY + rouletteCellH, rouletteCellW, rouletteCellH, Integer.toString(base - 1), "number:" + (base - 1)));
            rouletteCells.add(new RouletteCell(x, rouletteBoardY + rouletteCellH * 2, rouletteCellW, rouletteCellH, Integer.toString(base - 2), "number:" + (base - 2)));
        }
        int colX = numberX + numberW + 4;
        rouletteCells.add(new RouletteCell(colX, rouletteBoardY, columnW, rouletteCellH, "2:1", "column3"));
        rouletteCells.add(new RouletteCell(colX, rouletteBoardY + rouletteCellH, columnW, rouletteCellH, "2:1", "column2"));
        rouletteCells.add(new RouletteCell(colX, rouletteBoardY + rouletteCellH * 2, columnW, rouletteCellH, "2:1", "column1"));

        int dozenY = rouletteBoardY + rouletteCellH * 3 + 4;
        int dozenW = numberW / 3;
        rouletteCells.add(new RouletteCell(numberX, dozenY, dozenW, 27, "1ª DOCENA", "dozen1"));
        rouletteCells.add(new RouletteCell(numberX + dozenW, dozenY, dozenW, 27, "2ª DOCENA", "dozen2"));
        rouletteCells.add(new RouletteCell(numberX + dozenW * 2, dozenY, numberW - dozenW * 2, 27, "3ª DOCENA", "dozen3"));

        int outsideY = dozenY + 31;
        int outsideW = numberW / 6;
        String[] labels = {"1–18", "PAR", "ROJO", "NEGRO", "IMPAR", "19–36"};
        String[] actions = {"low", "even", "red", "black", "odd", "high"};
        for (int i = 0; i < 6; i++) {
            int x = numberX + i * outsideW;
            int w = i == 5 ? numberW - outsideW * 5 : outsideW;
            rouletteCells.add(new RouletteCell(x, outsideY, w, 27, labels[i], actions[i]));
        }
    }

    private void initGeneric() {
        sideW = Math.min(410, Math.max(290, panelW / 2 - 30));
        gameX = panelX + 24;
        gameW = sideW;
        sideX = panelX + panelW - sideW - 24;

        int inputY = contentTop + 70;
        amountField = new TextFieldWidget(textRenderer, gameX + 14, inputY, gameW - 28, 22, Text.literal("Cantidad"));
        amountField.setText(startAmount());
        amountField.setMaxLength(18);
        addDrawableChild(amountField);

        int quickY = inputY + 30;
        int qGap = 5;
        int qW = (gameW - 28 - qGap * 2) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("Mínimo"), ignored -> setQuickAmount(1))
                .dimensions(gameX + 14, quickY, qW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x5"), ignored -> setQuickAmount(5))
                .dimensions(gameX + 14 + qW + qGap, quickY, qW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("x10"), ignored -> setQuickAmount(10))
                .dimensions(gameX + 14 + (qW + qGap) * 2, quickY, qW, 20).build());

        List<Action> actions = actions();
        int actionsY = quickY + 36;
        int actionGap = 8;
        int buttonW = (gameW - 28 - actionGap) / 2;
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            int col = i % 2;
            int row = i / 2;
            addDrawableChild(ButtonWidget.builder(Text.literal(action.label), ignored -> send(action.action))
                    .dimensions(gameX + 14 + col * (buttonW + actionGap), actionsY + row * 29, buttonW, 22).build());
        }
    }

    private String startAmount() {
        return previousAmount == null || previousAmount.isBlank()
                ? Long.toString(Math.max(1L, state.minimumBet())) : previousAmount;
    }

    private void setQuickAmount(int multiplier) {
        long base = Math.max(1L, state.minimumBet());
        long amount = base > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : base * multiplier;
        if (amountField != null) amountField.setText(Long.toString(amount));
    }

    private boolean isRoulette() { return "roulette".equals(state.game()); }
    private boolean isBettingPhase() { return "idle".equals(state.phase()) || "betting".equals(state.phase()); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKDROP);
        context.fill(panelX - 4, panelY + 5, panelX + panelW + 4, panelY + panelH + 5, 0x99000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        context.fill(panelX, panelY, panelX + panelW, panelY + 5, GOLD_DARK);
        context.fill(panelX, panelY + 5, panelX + panelW, panelY + 8, GOLD);

        context.drawTextWithShadow(textRenderer, Text.literal(safe(state.title(), "Casino Emipokemon")), panelX + 24, panelY + 20, WHITE);
        context.drawTextWithShadow(textRenderer, Text.literal("MESA MULTIJUGADOR · SERVIDOR AUTORITATIVO"), panelX + 24, panelY + 40, 0xFF9FD8AF);
        String balance = state.balance() + " Michicoins";
        context.drawTextWithShadow(textRenderer, Text.literal(balance), panelX + panelW - 120 - textRenderer.getWidth(balance), panelY + 40, GOLD);

        if (isRoulette()) renderRoulette(context, mouseX, mouseY);
        else renderGeneric(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderRoulette(DrawContext context, int mouseX, int mouseY) {
        context.fill(gameX, contentTop, gameX + gameW, panelY + panelH - 20, FELT);
        outline(context, gameX, contentTop, gameW, panelY + panelH - 20 - contentTop, LINE);
        context.drawTextWithShadow(textRenderer, Text.literal("RULETA EUROPEA"), gameX + 12, contentTop + 10, GOLD);
        context.drawTextWithShadow(textRenderer, Text.literal("Haz clic directamente sobre el tapete para colocar tu ficha"), gameX + 12, contentTop + 26, WHITE);

        drawRouletteWheel(context);
        String selected = selectedAction();
        for (RouletteCell cell : rouletteCells) drawRouletteCell(context, cell, mouseX, mouseY, selected);

        context.fill(sideX, contentTop, sideX + sideW, panelY + panelH - 20, PANEL_2);
        outline(context, sideX, contentTop, sideW, panelY + panelH - 20 - contentTop, LINE);
        int y = contentTop + 14;
        context.drawTextWithShadow(textRenderer, Text.literal("RONDA"), sideX + 14, y, GOLD);
        y += 18;
        drawBadge(context, sideX + 14, y, phaseLabel() + timerLabel(), isBettingPhase() ? 0xFF8DE6B5 : GOLD);
        y += 30;

        context.drawTextWithShadow(textRenderer, Text.literal("APUESTA"), sideX + 14, y, MUTED);
        y += 18;
        context.drawTextWithShadow(textRenderer, Text.literal("Cantidad"), sideX + 14, y, WHITE);
        y += 70;
        context.drawTextWithShadow(textRenderer, Text.literal("Una ficha / apuesta por ronda"), sideX + 14, y, MUTED);
        y += 22;

        context.drawTextWithShadow(textRenderer, Text.literal("TU FICHA"), sideX + 14, y, GOLD);
        y += 17;
        String own = selected.isBlank() ? "Aún no has apostado" : betLabel(selected);
        drawWrapped(context, own, sideX + 14, y, sideW - 28, WHITE, y + 42);
        y += 50;

        context.fill(sideX + 14, y, sideX + sideW - 14, y + 1, LINE);
        y += 12;
        context.drawTextWithShadow(textRenderer, Text.literal("JUGADORES"), sideX + 14, y, MUTED);
        y += 17;
        List<String> players = state.players() == null ? List.of() : state.players();
        if (players.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("Esperando apuestas…"), sideX + 14, y, MUTED);
            y += 14;
        } else {
            for (String player : players) {
                if (y > panelY + panelH - 116) break;
                context.fill(sideX + 14, y + 2, sideX + 18, y + 10, GOLD_DARK);
                context.drawTextWithShadow(textRenderer, Text.literal(player), sideX + 23, y, WHITE);
                y += 14;
            }
        }

        int msgY = panelY + panelH - 88;
        context.fill(sideX + 10, msgY, sideX + sideW - 10, panelY + panelH - 30, 0xFF120E0D);
        outline(context, sideX + 10, msgY, sideW - 20, panelY + panelH - 30 - msgY, LINE);
        context.drawTextWithShadow(textRenderer, Text.literal("MESA"), sideX + 18, msgY + 8, GOLD);
        drawWrapped(context, safe(state.message(), "Listo para jugar"), sideX + 18, msgY + 23, sideW - 36, WHITE, panelY + panelH - 35);
    }

    private void drawRouletteWheel(DrawContext context) {
        drawCircle(context, wheelCx, wheelCy, wheelRadius + 18, GOLD_DARK);
        drawCircle(context, wheelCx, wheelCy, wheelRadius + 12, 0xFF432D1D);
        drawCircle(context, wheelCx, wheelCy, wheelRadius - 8, 0xFF10271D);
        drawCircle(context, wheelCx, wheelCy, 31, GOLD_DARK);
        drawCircle(context, wheelCx, wheelCy, 24, 0xFF271B14);

        double rotation = rouletteWheelRotation();
        for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
            int number = ROULETTE_WHEEL[i];
            double angle = rotation + TAU * i / ROULETTE_WHEEL.length - Math.PI / 2.0D;
            int px = wheelCx + (int)Math.round(Math.cos(angle) * wheelRadius);
            int py = wheelCy + (int)Math.round(Math.sin(angle) * wheelRadius);
            int color = number == 0 ? GREEN : RED_NUMBERS.contains(number) ? RED : BLACK;
            context.fill(px - 7, py - 5, px + 8, py + 6, color);
            outline(context, px - 7, py - 5, 15, 11, 0xFF6D5A42);
            String label = Integer.toString(number);
            context.drawTextWithShadow(textRenderer, Text.literal(label), px - textRenderer.getWidth(label) / 2, py - 4, WHITE);
        }

        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        boolean settled = "result".equals(state.phase()) && result >= 0 && elapsed >= 1800L;
        double ballAngle = settled ? -Math.PI / 2.0D : -rouletteWheelRotation() * 1.31D + 0.45D;
        int bx = wheelCx + (int)Math.round(Math.cos(ballAngle) * (wheelRadius + 12));
        int by = wheelCy + (int)Math.round(Math.sin(ballAngle) * (wheelRadius + 12));
        drawCircle(context, bx, by, 4, WHITE);

        String center = result >= 0 && "result".equals(state.phase()) ? Integer.toString(result) : "EMI";
        context.drawTextWithShadow(textRenderer, Text.literal(center), wheelCx - textRenderer.getWidth(center) / 2, wheelCy - 4, GOLD);
    }

    private double rouletteWheelRotation() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        if ("result".equals(state.phase()) && result >= 0) {
            int index = 0;
            for (int i = 0; i < ROULETTE_WHEEL.length; i++) if (ROULETTE_WHEEL[i] == result) { index = i; break; }
            double t = Math.min(1.0D, elapsed / 1800.0D);
            double eased = 1.0D - Math.pow(1.0D - t, 3.0D);
            double target = -TAU * index / ROULETTE_WHEEL.length;
            return (TAU * 5.0D + target) * eased;
        }
        return elapsed * 0.00115D;
    }

    private int rouletteResultNumber() {
        String value = safe(state.tableState(), "");
        int marker = value.indexOf("salió ");
        if (marker < 0) marker = value.indexOf("salio ");
        if (marker < 0) return -1;
        int start = marker + 6;
        while (start < value.length() && !Character.isDigit(value.charAt(start))) start++;
        int end = start;
        while (end < value.length() && Character.isDigit(value.charAt(end))) end++;
        if (start >= end) return -1;
        try { return Integer.parseInt(value.substring(start, end)); }
        catch (NumberFormatException ignored) { return -1; }
    }

    private void drawRouletteCell(DrawContext context, RouletteCell cell, int mouseX, int mouseY, String selected) {
        int color = rouletteCellColor(cell.action);
        context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, color);
        int border = cell.action.equals(selected) ? GOLD : LINE;
        outline(context, cell.x, cell.y, cell.w, cell.h, border);
        if (isBettingPhase() && cell.contains(mouseX, mouseY) && !cell.action.equals(selected)) {
            outline(context, cell.x + 1, cell.y + 1, Math.max(1, cell.w - 2), Math.max(1, cell.h - 2), WHITE);
        }
        context.drawTextWithShadow(textRenderer, Text.literal(cell.label),
                cell.x + (cell.w - textRenderer.getWidth(cell.label)) / 2,
                cell.y + (cell.h - 8) / 2, WHITE);
        if (cell.action.equals(selected)) drawChip(context, cell.x + cell.w - 8, cell.y + 8);
    }

    private int rouletteCellColor(String action) {
        if (action.startsWith("number:")) {
            int number = Integer.parseInt(action.substring(7));
            if (number == 0) return GREEN;
            return RED_NUMBERS.contains(number) ? RED : BLACK;
        }
        return switch (action) {
            case "red" -> RED;
            case "black" -> BLACK;
            case "column1", "column2", "column3", "dozen1", "dozen2", "dozen3" -> FELT_2;
            default -> FELT;
        };
    }

    private void drawChip(DrawContext context, int x, int y) {
        drawCircle(context, x, y, 6, CHIP);
        drawCircle(context, x, y, 3, 0xFF9E6E15);
        context.fill(x - 4, y, x + 5, y + 1, WHITE);
    }

    private void renderGeneric(DrawContext context) {
        context.fill(gameX, contentTop, gameX + gameW, panelY + panelH - 24, PANEL_2);
        context.fill(sideX, contentTop, sideX + sideW, panelY + panelH - 24, PANEL_2);
        outline(context, gameX, contentTop, gameW, panelY + panelH - 24 - contentTop, LINE);
        outline(context, sideX, contentTop, sideW, panelY + panelH - 24 - contentTop, LINE);

        context.drawTextWithShadow(textRenderer, Text.literal("APUESTA / ACCIONES"), gameX + 14, contentTop + 14, GOLD);
        context.drawTextWithShadow(textRenderer, Text.literal("Cantidad"), gameX + 14, contentTop + 54, MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal("ESTADO DE LA PARTIDA"), sideX + 14, contentTop + 14, GOLD);
        drawBadge(context, sideX + 14, contentTop + 34, phaseLabel() + timerLabel(), 0xFF8DE6B5);

        int y = contentTop + 68;
        context.drawTextWithShadow(textRenderer, Text.literal("Mesa"), sideX + 14, y, MUTED);
        y = drawWrapped(context, safe(state.tableState(), "Sin datos"), sideX + 14, y + 15, sideW - 28, WHITE, contentTop + 154);
        y = Math.max(contentTop + 164, y + 8);
        context.drawTextWithShadow(textRenderer, Text.literal("Tu estado"), sideX + 14, y, GOLD);
        y = drawWrapped(context, safe(state.privateState(), "Sin acción pendiente"), sideX + 14, y + 15, sideW - 28, WHITE, contentTop + 238);
        y = Math.max(contentTop + 250, y + 8);
        context.drawTextWithShadow(textRenderer, Text.literal("Jugadores"), sideX + 14, y, MUTED);
        y += 16;
        List<String> players = state.players() == null ? List.of() : state.players();
        if (players.isEmpty()) context.drawTextWithShadow(textRenderer, Text.literal("—"), sideX + 14, y, MUTED);
        else for (String player : players) {
            if (y > panelY + panelH - 105) break;
            context.drawTextWithShadow(textRenderer, Text.literal("• " + player), sideX + 14, y, WHITE);
            y += 14;
        }

        int messageY = panelY + panelH - 84;
        context.fill(panelX + 24, messageY, panelX + panelW - 24, panelY + panelH - 30, 0xFF100B0A);
        outline(context, panelX + 24, messageY, panelW - 48, panelY + panelH - 30 - messageY, LINE);
        drawWrapped(context, safe(state.message(), "Listo"), panelX + 34, messageY + 12, panelW - 68, WHITE, panelY + panelH - 34);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && isRoulette() && isBettingPhase()) {
            for (RouletteCell cell : rouletteCells) {
                if (cell.contains(mouseX, mouseY)) {
                    send(cell.action);
                    return true;
                }
            }
        }
        return false;
    }

    private List<Action> actions() {
        List<Action> result = new ArrayList<>();
        String game = safe(state.game(), "");
        String phase = safe(state.phase(), "single");
        if ("slot".equals(game)) {
            result.add(new Action("Girar", "spin"));
        } else if ("chip_exchange".equals(game)) {
            result.add(new Action("Comprar fichas", "buy"));
            result.add(new Action("Canjear fichas", "sell"));
        } else if ("ticket_exchange".equals(game)) {
            result.add(new Action("Comprar ticket", "buy_ticket"));
        } else if ("dice".equals(game) && isBettingPhase()) {
            result.add(new Action("Menos de 7", "under7"));
            result.add(new Action("Más de 7", "over7"));
            result.add(new Action("Exactamente 7", "exact7"));
        } else if ("blackjack".equals(game)) {
            if (isBettingPhase()) result.add(new Action("Unirse a la mano", "join"));
            else if ("blackjack".equals(phase)) {
                result.add(new Action("Pedir", "hit"));
                result.add(new Action("Plantarse", "stand"));
            }
        } else if ("poker".equals(game)) {
            if (isBettingPhase()) result.add(new Action("Entrar al bote", "join"));
            else if (phase.startsWith("poker_")) result.add(new Action("Retirarse", "fold"));
        }
        return result;
    }

    private void send(String action) {
        long amount;
        try { amount = Long.parseLong(amountField == null ? "0" : amountField.getText().strip()); }
        catch (Exception ignored) { amount = 0L; }
        ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload(state.blockPos(), action, amount));
    }

    private String selectedAction() {
        String value = safe(state.privateState(), "");
        String marker = "Tu apuesta: ";
        int start = value.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = value.indexOf(" ·", start);
        if (end < 0) end = value.length();
        return value.substring(start, end).strip();
    }

    private String betLabel(String action) {
        if (action.startsWith("number:")) return "Número exacto " + action.substring(7);
        return switch (action) {
            case "red" -> "Rojo";
            case "black" -> "Negro";
            case "even" -> "Par";
            case "odd" -> "Impar";
            case "low" -> "1–18";
            case "high" -> "19–36";
            case "dozen1" -> "Primera docena";
            case "dozen2" -> "Segunda docena";
            case "dozen3" -> "Tercera docena";
            case "column1" -> "Columna 1 · 2:1";
            case "column2" -> "Columna 2 · 2:1";
            case "column3" -> "Columna 3 · 2:1";
            default -> action;
        };
    }

    private String phaseLabel() {
        return switch (safe(state.phase(), "single")) {
            case "idle" -> "Mesa lista";
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

    private void drawBadge(DrawContext context, int x, int y, String text, int color) {
        int w = textRenderer.getWidth(text) + 14;
        context.fill(x, y, x + w, y + 18, 0xFF0F0B0A);
        outline(context, x, y, w, 18, LINE);
        context.drawTextWithShadow(textRenderer, Text.literal(text), x + 7, y + 5, color);
    }

    private int drawWrapped(DrawContext context, String value, int x, int y, int width, int color, int maxY) {
        if (value == null || value.isBlank()) return y;
        for (var line : textRenderer.wrapLines(Text.literal(value), width)) {
            if (y > maxY) break;
            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 13;
        }
        return y;
    }

    private void drawCircle(DrawContext context, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int)Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private void outline(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private record Action(String label, String action) { }
    private record RouletteCell(int x, int y, int w, int h, String label, String action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
''')
