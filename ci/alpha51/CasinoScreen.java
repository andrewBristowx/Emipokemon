package com.emipokemon.client.casino;

import com.emipokemon.Emipokemon;
import com.emipokemon.casino.CasinoNetworking;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

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
    private static final Identifier ROULETTE_HEADER = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_header.png");
    private static final Identifier ROULETTE_LEFT_PANEL = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_left_panel.png");
    private static final Identifier ROULETTE_SIDE_PANEL = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_side_panel.png");
    private static final Identifier ROULETTE_WHEEL_OUTER = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_wheel_outer.png");
    private static final Identifier ROULETTE_MEDALLION = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_medallion.png");
    private static final Identifier ROULETTE_ALPHA51_BACKGROUND = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_alpha51_background.png");
    private static final Identifier ROULETTE_ALPHA51_WHEEL = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/roulette_alpha51_wheel.png");
    private static final Identifier CASINO_FONT = Identifier.of(Emipokemon.MOD_ID, "casino");
    private static final int ROULETTE_HEADER_W = 1080;
    private static final int ROULETTE_HEADER_H = 103;
    private static final int ROULETTE_WHEEL_TEX_SIZE = 410;
    private static final int ROULETTE_MEDALLION_TEX_SIZE = 200;
    private static final int ROULETTE_ALPHA51_WHEEL_TEX_SIZE = 1254;
    private static final int ROULETTE_LEFT_W = 728;
    private static final int ROULETTE_LEFT_H = 572;
    private static final int ROULETTE_SIDE_W = 304;
    private static final int ROULETTE_SIDE_H = 572;
    private static final int ROULETTE_HEADER_TEX_W = 1535;
    private static final int ROULETTE_HEADER_TEX_H = 146;
    private static final int ROULETTE_LEFT_TEX_W = 1052;
    private static final int ROULETTE_LEFT_TEX_H = 828;
    private static final int ROULETTE_SIDE_TEX_W = 440;
    private static final int ROULETTE_SIDE_TEX_H = 828;
    private static final int ROULETTE_DISPLAY_CAPACITY = 8;
    private static final int ROULETTE_REFERENCE_W = 1536;
    private static final int ROULETTE_REFERENCE_H = 1024;
    private static final int ROULETTE_CONTENT_SOURCE_Y = 146;
    private static final int ROULETTE_SIDE_SOURCE_X = 1095;

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
    private final List<QuickChipZone> quickChipZones = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int gameX;
    private int gameW;
    private int sideX;
    private int sideW;
    private int contentTop;
    private int rouletteHeaderH;
    private int rouletteOverlapH;
    private int rouletteBoardY;
    private int rouletteCellW;
    private int rouletteCellH;
    private int wheelCx;
    private int wheelCy;
    private int wheelRadius;
    private int rouletteContentH;

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
        quickChipZones.clear();

        if (isRoulette()) {
            // Fit the original reference composition by both axes. Independent width/height scaling
            // distorted alpha.49, enlarged the wheel and cut the footer on wide displays.
            int availableW = Math.min(ROULETTE_REFERENCE_W, Math.max(1, width - 24));
            int availableH = Math.min(ROULETTE_REFERENCE_H, Math.max(1, height - 24));
            float referenceAspect = ROULETTE_REFERENCE_W / (float)ROULETTE_REFERENCE_H;
            if (availableW / (float)availableH > referenceAspect) {
                panelH = availableH;
                panelW = Math.max(1, Math.round(panelH * referenceAspect));
            } else {
                panelW = availableW;
                panelH = Math.max(1, Math.round(panelW / referenceAspect));
            }
        } else {
            panelW = Math.min(1080, Math.max(620, width - 20));
            panelH = Math.min(660, Math.max(430, height - 20));
        }
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        if (isRoulette()) {
            rouletteHeaderH = Math.max(1, Math.round(panelH * (ROULETTE_HEADER_TEX_H / (float)ROULETTE_REFERENCE_H)));
            contentTop = panelY + rouletteHeaderH;
            initRoulette();
        } else {
            contentTop = panelY + 68;
            addDrawableChild(new CasinoButtonWidget(panelX + panelW - 96, panelY + 20, 72, 22,
                    Text.literal("Cerrar"), this::close));
            initGeneric();
        }
    }

    private void initRoulette() {
        // alpha.51 uses one complete 1536x1024 composition. Interactive controls are
        // transparent hit zones placed over the art; no split panel is drawn on top.
        rouletteOverlapH = 0;
        contentTop = panelY + Math.round(panelH * (ROULETTE_CONTENT_SOURCE_Y / (float)ROULETTE_REFERENCE_H));
        rouletteContentH = Math.round(panelH * (ROULETTE_LEFT_TEX_H / (float)ROULETTE_REFERENCE_H));
        gameX = panelX;
        gameW = Math.round(panelW * (ROULETTE_LEFT_TEX_W / (float)ROULETTE_REFERENCE_W));
        sideX = panelX + Math.round(panelW * (ROULETTE_SIDE_SOURCE_X / (float)ROULETTE_REFERENCE_W));
        sideW = Math.round(panelW * (ROULETTE_SIDE_TEX_W / (float)ROULETTE_REFERENCE_W));

        int fieldX = panelX + refX(1169);
        int fieldY = panelY + refY(336);
        int fieldW = refX(258);
        amountField = new TextFieldWidget(textRenderer, fieldX, fieldY, fieldW, Math.max(16, refY(44)), Text.literal("Cantidad"));
        amountField.setText(startAmount());
        amountField.setMaxLength(18);
        amountField.setDrawsBackground(false);
        amountField.setEditableColor(WHITE);
        addDrawableChild(amountField);

        wheelCx = panelX + refX(627);
        wheelCy = panelY + refY(355);
        wheelRadius = Math.max(82, refX(219));
        rouletteBoardY = panelY + refY(548);
        buildRouletteCells();

        int chipRadius = Math.max(18, refX(48));
        quickChipZones.add(new QuickChipZone(panelX + refX(425), panelY + refY(872), chipRadius, 1, "MIN"));
        quickChipZones.add(new QuickChipZone(panelX + refX(574), panelY + refY(872), chipRadius, 5, "x5"));
        quickChipZones.add(new QuickChipZone(panelX + refX(719), panelY + refY(872), chipRadius, 10, "x10"));
    }

    private void buildRouletteCells() {
        int zeroX = panelX + refX(92);
        int zeroY = panelY + refY(548);
        int zeroW = refX(78);
        int numberX = panelX + refX(170);
        int numberW = refX(800);
        int columnX = panelX + refX(970);
        int columnW = refX(72);
        int numbersBottom = panelY + refY(708);

        rouletteCells.add(new RouletteCell(zeroX, zeroY, zeroW, numbersBottom - zeroY, "0", "number:0"));
        for (int column = 0; column < 12; column++) {
            int x1 = numberX + Math.round(numberW * (column / 12.0F));
            int x2 = numberX + Math.round(numberW * ((column + 1) / 12.0F));
            int base = (column + 1) * 3;
            for (int row = 0; row < 3; row++) {
                int y1 = zeroY + Math.round((numbersBottom - zeroY) * (row / 3.0F));
                int y2 = zeroY + Math.round((numbersBottom - zeroY) * ((row + 1) / 3.0F));
                int number = base - row;
                rouletteCells.add(new RouletteCell(x1, y1, x2 - x1, y2 - y1, Integer.toString(number), "number:" + number));
            }
        }
        for (int row = 0; row < 3; row++) {
            int y1 = zeroY + Math.round((numbersBottom - zeroY) * (row / 3.0F));
            int y2 = zeroY + Math.round((numbersBottom - zeroY) * ((row + 1) / 3.0F));
            rouletteCells.add(new RouletteCell(columnX, y1, columnW, y2 - y1, "2:1", "column" + (3 - row)));
        }

        int dozenX = panelX + refX(168);
        int dozenY = panelY + refY(709);
        int dozenW = refX(804);
        int dozenH = Math.max(18, refY(52));
        for (int i = 0; i < 3; i++) {
            int x1 = dozenX + Math.round(dozenW * (i / 3.0F));
            int x2 = dozenX + Math.round(dozenW * ((i + 1) / 3.0F));
            rouletteCells.add(new RouletteCell(x1, dozenY, x2 - x1, dozenH, (i + 1) + "ª DOCENA", "dozen" + (i + 1)));
        }

        int outsideY = panelY + refY(762);
        int outsideH = Math.max(18, refY(52));
        String[] labels = {"1–18", "PAR", "ROJO", "NEGRO", "IMPAR", "19–36"};
        String[] actions = {"low", "even", "red", "black", "odd", "high"};
        for (int i = 0; i < 6; i++) {
            int x1 = dozenX + Math.round(dozenW * (i / 6.0F));
            int x2 = dozenX + Math.round(dozenW * ((i + 1) / 6.0F));
            rouletteCells.add(new RouletteCell(x1, outsideY, x2 - x1, outsideH, labels[i], actions[i]));
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
        addDrawableChild(new CasinoButtonWidget(gameX + 14, quickY, qW, 20, Text.literal("Mínimo"), () -> setQuickAmount(1)));
        addDrawableChild(new CasinoButtonWidget(gameX + 14 + qW + qGap, quickY, qW, 20, Text.literal("x5"), () -> setQuickAmount(5)));
        addDrawableChild(new CasinoButtonWidget(gameX + 14 + (qW + qGap) * 2, quickY, qW, 20, Text.literal("x10"), () -> setQuickAmount(10)));

        List<Action> actions = actions();
        int actionsY = quickY + 36;
        int actionGap = 8;
        int buttonW = (gameW - 28 - actionGap) / 2;
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            int col = i % 2;
            int row = i / 2;
            addDrawableChild(new CasinoButtonWidget(gameX + 14 + col * (buttonW + actionGap), actionsY + row * 29,
                    buttonW, 22, Text.literal(action.label), () -> send(action.action)));
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

    private void adjustAmount(int direction) {
        long step = Math.max(1L, state.minimumBet());
        long current;
        try { current = Long.parseLong(amountField == null ? "0" : amountField.getText().strip()); }
        catch (Exception ignored) { current = step; }
        long amount = direction < 0
                ? Math.max(step, current - Math.min(current, step))
                : current > Long.MAX_VALUE - step ? Long.MAX_VALUE : current + step;
        if (amountField != null) amountField.setText(Long.toString(amount));
    }

    private boolean isRoulette() { return "roulette".equals(state.game()); }
    private boolean isBettingPhase() { return "idle".equals(state.phase()) || "betting".equals(state.phase()); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKDROP);
        context.fill(panelX - 4, panelY + 5, panelX + panelW + 4, panelY + panelH + 5, 0x99000000);

        if (isRoulette()) {
            // A single finished composition owns every static border, ornament and label.
            // Dynamic values and animation are the only elements rendered above it.
            drawAsset(context, ROULETTE_ALPHA51_BACKGROUND, panelX, panelY, panelW, panelH,
                    ROULETTE_REFERENCE_W, ROULETTE_REFERENCE_H);

            // Keep the real field active for input, but render its value once through our slot.
            boolean fieldWasVisible = amountField != null && amountField.visible;
            if (amountField != null) amountField.visible = false;
            super.render(context, mouseX, mouseY, delta);
            if (amountField != null) amountField.visible = fieldWasVisible;
            renderRoulette(context, mouseX, mouseY);
            return;
        }

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        outline(context, panelX, panelY, panelW, panelH, GOLD_DARK);
        outline(context, panelX + 2, panelY + 2, panelW - 4, panelH - 4, LINE);
        context.fill(panelX, panelY, panelX + panelW, panelY + 5, GOLD_DARK);
        context.fill(panelX, panelY + 5, panelX + panelW, panelY + 8, GOLD);
        String title = safe(state.title(), "Casino Emipokemon");
        context.drawTextWithShadow(textRenderer, Text.literal(title), panelX + 24, panelY + 20, WHITE);
        context.drawTextWithShadow(textRenderer, Text.literal("MESA MULTIJUGADOR · SERVIDOR AUTORITATIVO"), panelX + 24, panelY + 40, 0xFF9FD8AF);
        String balance = state.balance() + " Michicoins";
        context.drawTextWithShadow(textRenderer, Text.literal(balance), panelX + panelW - 122 - textRenderer.getWidth(balance), panelY + 34, GOLD);
        renderGeneric(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderRoulette(DrawContext context, int mouseX, int mouseY) {
        renderHeaderBalance(context);
        drawRouletteWheel(context);
        drawRecentResults(context);
        drawUiText(context, "Selecciona una casilla", panelX + refX(139), panelY + refY(307), WHITE, false);
        drawUiText(context, "para apostar", panelX + refX(139), panelY + refY(324), WHITE, false);

        String selected = selectedAction();
        for (RouletteCell cell : rouletteCells) drawRouletteCell(context, cell, mouseX, mouseY, selected);
        drawQuickChipControls(context, mouseX, mouseY);

        renderRoundState(context);
        renderWagerAmount(context);

        String selectedAmount = selectedBetAmount();
        String own = selected.isBlank() ? "AÚN NO HAS APOSTADO"
                : betLabel(selected).toUpperCase(java.util.Locale.ROOT) + (selectedAmount.isBlank() ? "" : " · " + selectedAmount);
        drawIntegratedWrapped(context, own, sideX + sidePx(35), contentTop + sidePy(266), sidePx(235), WHITE, contentTop + sidePy(307));
        if (selected.isBlank()) {
            drawUiText(context, "Selecciona una casilla en el tapete", sideX + sidePx(35), contentTop + sidePy(291), MUTED, false);
        }

        List<String> players = state.players() == null ? List.of() : state.players();
        renderPlayerCount(context, players.size());
        drawPlayers(context, contentTop + sidePy(365), contentTop + sidePy(449));

        drawIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                sideX + sidePx(33), contentTop + sidePy(507), sidePx(222), WHITE, contentTop + sidePy(548));
    }

    private void renderHeaderBalance(DrawContext context) {
        // The HD header already contains the coin and an empty burgundy balance capsule.
        int x = panelX + headerPx(1176);
        int w = Math.max(76, headerPx(172));
        int y = panelY + headerPy(72);
        String value = state.balance() + " Michicoins";
        drawFittedCenteredUiText(context, value, x + w / 2, y, w - headerPx(12), 0xFFFFDF62, false);
    }

    private void renderRoundState(DrawContext context) {
        int x = panelX + refX(1126);
        int y = panelY + refY(196);
        int badgeW = refX(127);
        drawCenteredUiText(context, phaseLabel(), x + badgeW / 2, y + refY(9),
                isBettingPhase() ? 0xFF9AF2B6 : 0xFFFFDB68, false);
        String timer = timerOnly();
        if (!timer.isBlank()) {
            int timerCenter = panelX + refX(1374);
            drawCenteredUiText(context, "Inicia en:", timerCenter, y, 0xFFF7F0E4, false);
            drawCenteredUiText(context, timer, timerCenter, y + refY(18), 0xFFFFE9A1, false);
        }
    }

    private void renderWagerAmount(DrawContext context) {
        int x = panelX + refX(1169);
        int y = panelY + refY(336);
        int w = refX(258);
        int h = Math.max(16, refY(44));
        String value = amountField == null ? startAmount() : amountField.getText();
        int textY = y + Math.max(4, (h - 8) / 2);
        int coinX = x + sidePx(12);
        drawCoin(context, coinX, y + h / 2, Math.max(3, sidePx(5)));
        int textX = x + sidePx(23);
        drawUiText(context, value, textX, textY, 0xFFF7F0E4, false);

        if (amountField != null && amountField.isFocused() && ((System.currentTimeMillis() / 450L) & 1L) == 0L) {
            int caretX = Math.min(x + w - sidePx(8), textX + textRenderer.getWidth(casinoText(value, false)) + 1);
            context.fill(caretX, textY - 1, caretX + 1, textY + 10, 0xFFFFE7A0);
        }
    }

    private void renderPlayerCount(DrawContext context, int count) {
        int x = panelX + refX(1432);
        int y = panelY + refY(657);
        int w = refX(65);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawFittedCenteredUiText(context, value, x + w / 2, y + refY(7),
                w - refX(8), 0xFFFFD85B, false);
    }

    private Text casinoText(String value, boolean bold) {
        return Text.literal(value).setStyle(Style.EMPTY.withFont(CASINO_FONT).withBold(bold));
    }

    private void drawUiText(DrawContext context, String value, int x, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawCenteredUiText(DrawContext context, String value, int centerX, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        context.drawTextWithShadow(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    private void drawFittedCenteredUiText(DrawContext context, String value, int centerX, int y,
                                          int maxWidth, int color, boolean bold) {
        Text text = casinoText(value, bold);
        int textWidth = Math.max(1, textRenderer.getWidth(text));
        float scale = Math.min(1.0F, maxWidth / (float)textWidth);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawTextWithShadow(textRenderer, text, -textWidth / 2, 0, color);
        context.getMatrices().pop();
    }

    private int drawIntegratedWrapped(DrawContext context, String value, int x, int y, int width, int color, int maxY) {
        if (value == null || value.isBlank()) return y;
        for (var line : textRenderer.wrapLines(casinoText(value, false), width)) {
            if (y > maxY) break;
            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 13;
        }
        return y;
    }

    private void drawRouletteWheel(DrawContext context) {
        int size = Math.max(180, refX(438));
        double rotation = rouletteWheelRotation();
        float numberScale = Math.max(0.44F, Math.min(0.72F, size / 438.0F * 0.72F));
        int numberRadius = Math.round(size * 0.386F);

        context.getMatrices().push();
        context.getMatrices().translate(wheelCx, wheelCy, 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)rotation));
        drawAsset(context, ROULETTE_ALPHA51_WHEEL, -size / 2, -size / 2, size, size,
                ROULETTE_ALPHA51_WHEEL_TEX_SIZE, ROULETTE_ALPHA51_WHEEL_TEX_SIZE);

        // The generated wheel deliberately has blank pockets. The exact canonical
        // European order is written here, so artwork can never invent a result ID.
        for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
            int number = ROULETTE_WHEEL[i];
            double angle = TAU * i / ROULETTE_WHEEL.length - Math.PI / 2.0D;
            int px = (int)Math.round(Math.cos(angle) * numberRadius);
            int py = (int)Math.round(Math.sin(angle) * numberRadius);
            String label = Integer.toString(number);
            context.getMatrices().push();
            context.getMatrices().translate(px, py, 0.0F);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)(angle + Math.PI / 2.0D)));
            context.getMatrices().scale(numberScale, numberScale, 1.0F);
            Text numberText = casinoText(label, true);
            context.drawTextWithShadow(textRenderer, numberText, -textRenderer.getWidth(numberText) / 2, -4, WHITE);
            context.getMatrices().pop();
        }
        context.getMatrices().pop();

        // The logo remains readable while the mechanical wheel underneath it rotates.
        int medSize = refX(116);
        drawAsset(context, ROULETTE_MEDALLION, wheelCx - medSize / 2, wheelCy - medSize / 2, medSize, medSize, ROULETTE_MEDALLION_TEX_SIZE, ROULETTE_MEDALLION_TEX_SIZE);
        drawRouletteBall(context, size);
    }

    private void drawRouletteBall(DrawContext context, int wheelSize) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        boolean settling = "result".equals(state.phase()) && result >= 0;
        double t = settling ? Math.min(1.0D, elapsed / 3200.0D) : 0.0D;
        double eased = 1.0D - Math.pow(1.0D - t, 4.0D);
        double freeAngle = -Math.PI / 2.0D - elapsed * 0.0047D;
        double targetAngle = -Math.PI / 2.0D - TAU * 3.0D;
        double angle = freeAngle * (1.0D - eased) + targetAngle * eased;
        double radius = wheelSize * (0.456D - 0.055D * eased);
        int bx = wheelCx + (int)Math.round(Math.cos(angle) * radius);
        int by = wheelCy + (int)Math.round(Math.sin(angle) * radius);
        int ballRadius = Math.max(3, refX(8));
        drawCircle(context, bx + 1, by + 2, ballRadius + 1, 0x66000000);
        drawCircle(context, bx, by, ballRadius, 0xFFF8F7EE);
        drawCircleOutline(context, bx, by, ballRadius, 0xFFFFE8A3);
    }

    private double rouletteWheelRotation() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        if ("result".equals(state.phase()) && result >= 0) {
            int index = 0;
            for (int i = 0; i < ROULETTE_WHEEL.length; i++) if (ROULETTE_WHEEL[i] == result) { index = i; break; }
            double t = Math.min(1.0D, elapsed / 3200.0D);
            double eased = 1.0D - Math.pow(1.0D - t, 4.0D);
            double target = -TAU * index / ROULETTE_WHEEL.length;
            return (TAU * 7.0D + target) * eased;
        }
        return elapsed * 0.00135D;
    }

    private int rouletteResultNumber() {
        List<Integer> recent = state.recentResults();
        if ("result".equals(state.phase()) && recent != null && !recent.isEmpty()) return recent.getFirst();
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
        if (cell.action.equals(selected)) {
            outline(context, cell.x, cell.y, cell.w, cell.h, 0xFFFFD75A);
            if (cell.w > 5 && cell.h > 5) outline(context, cell.x + 2, cell.y + 2, cell.w - 4, cell.h - 4, 0xFFFFF1A8);
            drawChip(context, cell.x + cell.w - Math.max(6, leftPx(8)), cell.y + Math.max(7, leftPy(9)));
        } else if (isBettingPhase() && cell.contains(mouseX, mouseY)) {
            outline(context, cell.x, cell.y, cell.w, cell.h, WHITE);
            if (cell.w > 5 && cell.h > 5) outline(context, cell.x + 2, cell.y + 2, cell.w - 4, cell.h - 4, 0xAAFFFFCC);
        }
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
        drawCasinoChip(context, x, y, 6, 0xFFB93636);
    }

    private void drawRecentResults(DrawContext context) {
        List<Integer> recent = state.recentResults() == null ? List.of() : state.recentResults();
        int count = Math.min(5, recent.size());
        int[] centers = {863, 906, 948, 991, 1033};
        int radius = Math.max(7, refX(15));
        for (int i = 0; i < count; i++) {
            int number = recent.get(i);
            int bx = panelX + refX(centers[i]);
            int by = panelY + refY(270);
            int color = number == 0 ? 0xFF07854D : RED_NUMBERS.contains(number) ? 0xFFD92735 : 0xFF171318;
            drawCircle(context, bx, by, radius, color);
            drawCircleOutline(context, bx, by, radius, i == 0 ? GOLD : 0xFFD39A39);
            String label = Integer.toString(number);
            drawCenteredUiText(context, label, bx, by - 4, WHITE, true);
        }
    }

    private void drawQuickChipControls(DrawContext context, int mouseX, int mouseY) {
        // The finished artwork supplies the visible chips. These controls are deliberately
        // invisible; quickChipZones keep the same buttons fully interactive.
    }

    private void drawPlayers(DrawContext context, int y, int maxY) {
        List<String> players = state.players() == null ? List.of() : state.players();
        if (players.isEmpty()) {
            context.drawTextWithShadow(textRenderer, casinoText("Esperando apuestas…", false), sideX + sidePx(34), y, MUTED);
            return;
        }
        int shown = 0;
        for (String player : players) {
            if (y + 14 > maxY || shown >= 5) break;
            drawCircle(context, sideX + sidePx(29), y + 5, Math.max(3, sidePx(5)), 0xFF485078);
            String clipped = textRenderer.trimToWidth(player, sideW - sidePx(58));
            context.drawTextWithShadow(textRenderer, casinoText(clipped, false), sideX + sidePx(41), y + 1, WHITE);
            y += Math.max(13, sidePy(18));
            shown++;
        }
        if (players.size() > shown && y <= maxY) {
            context.drawTextWithShadow(textRenderer, casinoText("+" + (players.size() - shown) + " más", false), sideX + sidePx(41), y, MUTED);
        }
    }

    private void drawInfoBox(DrawContext context, int x, int y, int w, int h, String title) {
        drawFramedPanel(context, x, y, w, h, PANEL_2, GOLD_DARK);
        context.fill(x + 1, y + 1, x + w - 1, y + 22, 0xFF2A1B16);
        context.fill(x + 1, y + 21, x + w - 1, y + 22, LINE);
        context.drawTextWithShadow(textRenderer, Text.literal(title), x + 14, y + 7, GOLD);
    }

    private void drawFramedPanel(DrawContext context, int x, int y, int w, int h, int fill, int border) {
        context.fill(x, y, x + w, y + h, fill);
        outline(context, x, y, w, h, border);
        if (w > 4 && h > 4) outline(context, x + 2, y + 2, w - 4, h - 4, LINE);
    }

    private void drawCaptureBall(DrawContext context, int cx, int cy, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int)Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            int color = dy < 0 ? RED : WHITE;
            context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
        context.fill(cx - radius, cy - 1, cx + radius + 1, cy + 2, BLACK);
        drawCircle(context, cx, cy, Math.max(2, radius / 3), BLACK);
        drawCircle(context, cx, cy, Math.max(1, radius / 5), WHITE);
        drawCircleOutline(context, cx, cy, radius, GOLD_DARK);
    }

    private void drawCasinoChip(DrawContext context, int cx, int cy, int radius, int accent) {
        drawCircle(context, cx, cy, radius, 0xFF4A352A);
        drawCircle(context, cx, cy, Math.max(1, radius - 2), accent);
        drawCircle(context, cx, cy, Math.max(1, radius - 6), WHITE);
        context.fill(cx - radius + 2, cy - 1, cx + radius - 1, cy + 2, accent);
        context.fill(cx - 1, cy - radius + 2, cx + 2, cy + radius - 1, accent);
    }

    private void drawCoin(DrawContext context, int cx, int cy, int radius) {
        drawCircle(context, cx, cy, radius, GOLD_DARK);
        drawCircle(context, cx, cy, Math.max(1, radius - 1), GOLD);
        if (radius >= 4) context.fill(cx - 1, cy - 2, cx + 2, cy + 3, GOLD_DARK);
    }

    private void drawCoinStack(DrawContext context, int x, int y) {
        drawCoin(context, x - 8, y + 4, 6);
        drawCoin(context, x, y + 8, 6);
        drawCoin(context, x + 8, y + 4, 6);
        drawCoin(context, x, y - 1, 6);
    }

    private void drawCircleOutline(DrawContext context, int cx, int cy, int radius, int color) {
        int inner = Math.max(0, radius - 1);
        for (int dy = -radius; dy <= radius; dy++) {
            int outerDx = (int)Math.floor(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            int innerDx = Math.abs(dy) > inner ? -1 : (int)Math.floor(Math.sqrt(Math.max(0, inner * inner - dy * dy)));
            if (innerDx < 0) context.fill(cx - outerDx, cy + dy, cx + outerDx + 1, cy + dy + 1, color);
            else {
                context.fill(cx - outerDx, cy + dy, cx - innerDx, cy + dy + 1, color);
                context.fill(cx + innerDx + 1, cy + dy, cx + outerDx + 1, cy + dy + 1, color);
            }
        }
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
        if (button == 0 && isRoulette()) {
            if (inside(mouseX, mouseY, panelX + refX(1357), panelY + refY(66), refX(133), refY(51))) {
                close();
                return true;
            }
            int amountY = panelY + refY(336);
            if (inside(mouseX, mouseY, panelX + refX(1127), amountY, refX(41), refY(45))) { adjustAmount(-1); return true; }
            if (inside(mouseX, mouseY, panelX + refX(1428), amountY, refX(42), refY(45))) { adjustAmount(1); return true; }
            int qy = panelY + refY(392);
            int qh = refY(42);
            if (inside(mouseX, mouseY, panelX + refX(1127), qy, refX(103), qh)) { setQuickAmount(1); return true; }
            if (inside(mouseX, mouseY, panelX + refX(1244), qy, refX(100), qh)) { setQuickAmount(5); return true; }
            if (inside(mouseX, mouseY, panelX + refX(1355), qy, refX(112), qh)) { setQuickAmount(10); return true; }
            for (QuickChipZone chip : quickChipZones) {
                if (chip.contains(mouseX, mouseY)) {
                    setQuickAmount(chip.multiplier);
                    return true;
                }
            }
            if (isBettingPhase()) {
                for (RouletteCell cell : rouletteCells) {
                    if (cell.contains(mouseX, mouseY)) {
                        send(cell.action);
                        return true;
                    }
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

    private String selectedBetAmount() {
        String value = safe(state.privateState(), "");
        String marker = " · ";
        int start = value.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = value.indexOf(" Michicoins", start);
        if (end < 0) return "";
        String amount = value.substring(start, end).strip();
        return amount.chars().allMatch(Character::isDigit) ? amount : "";
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

    private int leftPx(int px) { return Math.max(1, Math.round(px * gameW / (float)ROULETTE_LEFT_W)); }
    private int leftPy(int py) { return Math.max(1, Math.round(py * rouletteContentH / (float)ROULETTE_LEFT_H)); }
    private int sidePx(int px) { return Math.max(1, Math.round(px * sideW / (float)ROULETTE_SIDE_W)); }
    private int sidePy(int py) { return Math.max(1, Math.round(py * rouletteContentH / (float)ROULETTE_SIDE_H)); }

    private int headerPx(int px) { return Math.max(1, Math.round(px * panelW / (float)ROULETTE_HEADER_TEX_W)); }
    private int headerPy(int py) { return Math.max(1, Math.round(py * rouletteHeaderH / (float)ROULETTE_HEADER_TEX_H)); }
    private int refX(int px) { return Math.max(1, Math.round(px * panelW / (float)ROULETTE_REFERENCE_W)); }
    private int refY(int py) { return Math.max(1, Math.round(py * panelH / (float)ROULETTE_REFERENCE_H)); }

    private void drawAsset(DrawContext context, Identifier texture, int x, int y, int w, int h, int textureW, int textureH) {
        context.drawTexture(texture, x, y, w, h, 0.0F, 0.0F, textureW, textureH, textureW, textureH);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private String timerOnly() {
        if (state.deadlineMillis() <= 0L) return "";
        long left = Math.max(0L, (state.deadlineMillis() - System.currentTimeMillis() + 999L) / 1000L);
        return String.format("%02d:%02d", left / 60L, left % 60L);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Deliberately empty: the casino draws its own dim backdrop in render().
        // Allowing Screen.renderBackground() here re-applies Cobbleverse/Minecraft blur
        // over the casino UI and makes text, roulette numbers and widgets out of focus.
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private record Action(String label, String action) { }
    private record QuickChipZone(int x, int y, int radius, int multiplier, String label) {
        boolean contains(double mouseX, double mouseY) {
            int dx = (int)Math.round(mouseX) - x;
            int dy = (int)Math.round(mouseY) - y;
            return dx * dx + dy * dy <= radius * radius;
        }
    }
    private record RouletteCell(int x, int y, int w, int h, String label, String action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
