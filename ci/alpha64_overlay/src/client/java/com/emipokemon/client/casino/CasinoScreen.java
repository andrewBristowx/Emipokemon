package com.emipokemon.client.casino;

import com.emipokemon.Emipokemon;
import com.emipokemon.casino.CasinoNetworking;
import com.emipokemon.client.render.PokemonPortraitRenderer;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Identifier CASINO_POKER = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_poker.png");
    private static final Identifier CASINO_BLACKJACK = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_blackjack.png");
    private static final Identifier CASINO_DICE = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_dice.png");
    private static final Identifier CASINO_SLOT = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_slot.png");
    private static final Identifier CASINO_SLOT_SYMBOLS = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/slot_symbols.png");
    private static final Identifier CASINO_CHIP_EXCHANGE = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_chip_exchange.png");
    private static final Identifier CASINO_TICKET_EXCHANGE = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_ticket_exchange.png");
    private static final Identifier CASINO_CLAW = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_claw.png");
    private static final Identifier CASINO_POKEMON_FLIP = Identifier.of(Emipokemon.MOD_ID, "textures/gui/casino/finished/casino_pokemon_flip.png");
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
    private static final int SLOT_SYMBOL_CELL_W = 256;
    private static final int SLOT_SYMBOL_CELL_H = 286;
    private static final int SLOT_SYMBOL_ATLAS_W = 1280;
    private static final int SLOT_SYMBOL_ATLAS_H = 286;
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
    private static final int MUTED = 0xFFE2D8C7;
    private static final int LINE = 0xFF6B523B;
    private static final int CHIP = 0xFFFFC94D;

    private final Screen parent;
    private final CasinoNetworking.CasinoState state;
    private final String previousAmount;
    private final PresentationState presentation;
    private final long openedAt;

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

    CasinoScreen(Screen parent, String json, String previousAmount, PresentationState previousPresentation) {
        super(Text.literal("Casino Emipokemon"));
        this.parent = parent;
        this.state = GSON.fromJson(json, CasinoNetworking.CasinoState.class);
        this.previousAmount = previousAmount;
        String signature = animationSignature(this.state);
        this.presentation = previousPresentation != null && previousPresentation.signature.equals(signature)
                ? previousPresentation : new PresentationState(signature, System.currentTimeMillis());
        this.openedAt = presentation.startedAt;
    }

    Screen parentScreen() { return parent; }
    String amountText() { return amountField == null ? previousAmount : amountField.getText(); }
    PresentationState presentationState() { return presentation; }

    private static String animationSignature(CasinoNetworking.CasinoState value) {
        return safe(value.game(), "") + '|' + safe(value.phase(), "") + '|' + value.roundId() + '|'
                + safe(value.tableState(), "") + '|' + safe(value.privateState(), "") + '|'
                + safe(value.message(), "") + '|' + String.valueOf(value.recentResults());
    }

    @Override
    protected void init() {
        rouletteCells.clear();
        quickChipZones.clear();

        if (isFinishedCasino()) {
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
        }
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        if (isRoulette()) {
            rouletteHeaderH = Math.max(1, Math.round(panelH * (ROULETTE_HEADER_TEX_H / (float)ROULETTE_REFERENCE_H)));
            contentTop = panelY + rouletteHeaderH;
            initRoulette();
        } else {
            initFinishedGame();
        }
    }

    private void initFinishedGame() {
        contentTop = panelY + refY(145);
        gameX = panelX + refX(42);
        gameW = refX(962);
        sideX = panelX + refX(1035);
        sideW = refX(443);
        if ("claw".equals(state.game()) || "pokemon_flip".equals(state.game())) {
            amountField = null;
            return;
        }
        amountField = new TextFieldWidget(textRenderer, sideX + refX(35), panelY + refY(245),
                sideW - refX(70), Math.max(18, refY(34)), Text.literal("Cantidad"));
        amountField.setText(startAmount());
        amountField.setMaxLength(18);
        amountField.setDrawsBackground(false);
        amountField.setEditableColor(WHITE);
        addDrawableChild(amountField);
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
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), 0.96F + multiplier * 0.015F);
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
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), direction < 0 ? 0.86F : 1.08F);
    }

    private boolean isRoulette() { return "roulette".equals(state.game()); }
    private boolean isFinishedCasino() { return isRoulette() || finishedAsset() != null; }
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
            updateCasinoSounds();
            return;
        }

        Identifier finished = finishedAsset();
        if (finished != null) {
            drawAsset(context, finished, panelX, panelY, panelW, panelH,
                    ROULETTE_REFERENCE_W, ROULETTE_REFERENCE_H);
            boolean fieldWasVisible = amountField != null && amountField.visible;
            if (amountField != null) amountField.visible = false;
            super.render(context, mouseX, mouseY, delta);
            if (amountField != null) amountField.visible = fieldWasVisible;
            renderFinishedGame(context, mouseX, mouseY);
            updateCasinoSounds();
            return;
        }

        CasinoTheme theme = casinoTheme();
        drawCasinoFrame(context, theme);
        renderGeneric(context, theme);
        super.render(context, mouseX, mouseY, delta);
    }

    private Identifier finishedAsset() {
        return switch (safe(state.game(), "")) {
            case "poker" -> CASINO_POKER;
            case "blackjack" -> CASINO_BLACKJACK;
            case "dice" -> CASINO_DICE;
            case "slot" -> CASINO_SLOT;
            case "chip_exchange" -> CASINO_CHIP_EXCHANGE;
            case "ticket_exchange" -> CASINO_TICKET_EXCHANGE;
            case "claw" -> CASINO_CLAW;
            case "pokemon_flip" -> CASINO_POKEMON_FLIP;
            default -> null;
        };
    }

    private void renderFinishedGame(DrawContext context, int mouseX, int mouseY) {
        String game = safe(state.game(), "");
        if ("claw".equals(game)) {
            drawFittedCenteredUiText(context, "MÁQUINA DE GARRA", panelX + refX(730), panelY + refY(76),
                    refX(430), WHITE, true);
        } else if (!"pokemon_flip".equals(game)) {
            drawFittedCenteredUiText(context, casinoTheme().title(), panelX + refX(390), panelY + refY(76),
                    refX(270), WHITE, true);
        }
        if (!"claw".equals(game) && !"pokemon_flip".equals(game))
            drawFittedCenteredUiText(context, state.balance() + " Michicoins", panelX + refX(1220), panelY + refY(76),
                    refX(230), 0xFFFFD85B, false);
        drawFittedCenteredUiText(context, "CERRAR", panelX + refX(1432), panelY + refY(76),
                refX(92), WHITE, true);

        if ("claw".equals(game)) {
            renderClawPanels(context);
            drawFinishedGameState(context);
            renderFinishedControls(context, mouseX, mouseY);
            return;
        }
        if ("pokemon_flip".equals(game)) {
            renderPokemonFlipPanels(context);
            drawFinishedGameState(context);
            renderFinishedControls(context, mouseX, mouseY);
            return;
        }

        int sx = panelX + refX(1060);
        int sw = refX(390);
        String roundHeader = "RONDA · " + phaseLabel() + timerLabel();
        drawFittedCenteredUiText(context, roundHeader, sx + sw / 2,
                panelY + refY(178), sw - refX(20), 0xFFFFD85B, true);
        drawFittedCenteredUiText(context, "APUESTA: " + (amountField == null ? startAmount() : amountField.getText()),
                sx + sw / 2, panelY + refY(230), sw - refX(30), WHITE, true);
        drawFinishedQuickControls(context, mouseX, mouseY);

        drawFittedCenteredUiText(context, "MESA", sx + sw / 2, panelY + refY(365), sw - refX(20), 0xFFFFD85B, true);
        drawWrapped(context, safe(state.tableState(), "Mesa lista"), sx + refX(18), panelY + refY(405),
                sw - refX(36), WHITE, panelY + refY(500));
        drawFittedCenteredUiText(context, "TU ESTADO", sx + sw / 2, panelY + refY(547), sw - refX(20), 0xFFFFD85B, true);
        drawWrapped(context, safe(state.privateState(), "Sin acción pendiente"), sx + refX(18), panelY + refY(588),
                sw - refX(36), WHITE, panelY + refY(690));
        drawFittedCenteredUiText(context, "JUGADORES", sx + sw / 2, panelY + refY(730), sw - refX(20), 0xFFFFD85B, true);
        int py = panelY + refY(770);
        List<String> players = state.players() == null ? List.of() : state.players();
        if (players.isEmpty()) drawUiText(context, "Esperando jugadores…", sx + refX(20), py, MUTED, false);
        else for (String player : players) {
            if (py > panelY + refY(820)) break;
            drawUiText(context, "• " + compactLabel(player, 25), sx + refX(20), py, WHITE, false);
            py += refY(22);
        }
        context.fill(sx + refX(18), panelY + refY(826), sx + sw - refX(18), panelY + refY(828), 0xAAFFD166);
        drawScaledIntegratedWrapped(context, safe(state.message(), "Listo"), sx + refX(18), panelY + refY(840),
                sw - refX(36), 0xFFB9EFD2, panelY + refY(910), 0.82F);

        drawFinishedGameState(context);
        renderFinishedControls(context, mouseX, mouseY);
    }

    private void renderFinishedControls(DrawContext context, int mouseX, int mouseY) {
        for (FinishedControl control : finishedActionControls()) {
            boolean hovered = control.contains(mouseX, mouseY);
            if ("claw".equals(state.game()) || "pokemon_flip".equals(state.game())) {
                int fill = hovered ? 0xE04C175E : 0xC0181530;
                fillRoundedRect(context, control.x(), control.y(), control.w(), control.h(), refX(9), fill);
                outline(context, control.x(), control.y(), control.w(), control.h(), hovered ? 0xFFFFFFFF : 0xFFFFD166);
            }
            drawFinishedControlLabel(context, control, hovered ? 0xFFFFFFFF : 0xFFFFE6A0);
        }
    }

    private void renderClawPanels(DrawContext context) {
        int sx = panelX + refX(1050);
        int sw = refX(400);
        drawFittedCenteredUiText(context, "MÁQUINA DE GARRA", sx + sw / 2, panelY + refY(178), sw - refX(24), 0xFFFFD85B, true);
        drawFittedCenteredUiText(context, "COSTO: 1 TICKET DE GARRA", sx + sw / 2, panelY + refY(230), sw - refX(32), WHITE, true);
        drawFittedCenteredUiText(context, "POKÉDOLLS REALES", sx + sw / 2, panelY + refY(365), sw - refX(24), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Sin premios"), sx + refX(28), panelY + refY(405),
                sw - refX(56), WHITE, panelY + refY(485), 0.88F);
        drawFittedCenteredUiText(context, "CONTROL", sx + sw / 2, panelY + refY(547), sw - refX(24), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.privateState(), "Mueve la garra"), sx + refX(28), panelY + refY(590),
                sw - refX(56), WHITE, panelY + refY(680), 0.88F);
        drawFittedCenteredUiText(context, "RESULTADO", sx + sw / 2, panelY + refY(730), sw - refX(24), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.message(), "Listo"), sx + refX(28), panelY + refY(778),
                sw - refX(56), 0xFFB9EFD2, panelY + refY(900), 0.82F);
    }

    private void renderPokemonFlipPanels(DrawContext context) {
        List<CasinoNetworking.PokemonDisplay> displays = state.pokemonDisplays() == null ? List.of() : state.pokemonDisplays();
        String left = displays.size() > 0 ? displays.get(0).playerName() : "JUGADOR 1";
        String right = displays.size() > 1 ? displays.get(1).playerName() : "JUGADOR 2";
        drawFittedCenteredUiText(context, left, panelX + refX(326), panelY + refY(79), refX(420), WHITE, true);
        drawFittedCenteredUiText(context, right, panelX + refX(887), panelY + refY(79), refX(280), WHITE, true);

        int sx = panelX + refX(1125);
        int sw = refX(330);
        drawFittedCenteredUiText(context, "DUELO · MEJOR DE 3", sx + sw / 2, panelY + refY(178), sw - refX(22), 0xFFFFD85B, true);
        drawFittedCenteredUiText(context, "POKÉMON EN CUSTODIA SEGURA", sx + sw / 2, panelY + refY(230), sw - refX(28), WHITE, true);
        drawFittedCenteredUiText(context, "CONSENTIMIENTO", sx + sw / 2, panelY + refY(365), sw - refX(20), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Esperando jugadores"), sx + refX(22), panelY + refY(405),
                sw - refX(44), WHITE, panelY + refY(485), 0.80F);
        drawFittedCenteredUiText(context, "TU ESTADO", sx + sw / 2, panelY + refY(547), sw - refX(20), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.privateState(), "Sin selección"), sx + refX(22), panelY + refY(588),
                sw - refX(44), WHITE, panelY + refY(680), 0.80F);
        drawFittedCenteredUiText(context, "MENSAJE", sx + sw / 2, panelY + refY(730), sw - refX(20), 0xFFFFD85B, true);
        drawScaledIntegratedWrapped(context, safe(state.message(), "Mesa lista"), sx + refX(22), panelY + refY(775),
                sw - refX(44), 0xFFB9EFD2, panelY + refY(900), 0.78F);
    }

    private void drawFinishedControlLabel(DrawContext context, FinishedControl control, int color) {
        String label = control.label().toUpperCase(java.util.Locale.ROOT);
        String first = label;
        String second = "";
        if ("exact7".equals(control.action())) {
            first = "EXACTAMENTE";
            second = "7";
        } else if ("join".equals(control.action()) && "blackjack".equals(state.game())) {
            first = "UNIRSE A";
            second = "LA MANO";
        } else if ("join".equals(control.action()) && "poker".equals(state.game())) {
            first = "ENTRAR";
            second = "AL BOTE";
        }
        int centerX = control.x() + control.w() / 2;
        int maxWidth = control.w() - refX(32);
        if (second.isBlank()) {
            drawFittedCenteredUiText(context, first, centerX, control.y() + control.h() / 2 - refY(6), maxWidth, color, true);
        } else {
            drawFittedCenteredUiText(context, first, centerX, control.y() + control.h() / 2 - refY(15), maxWidth, color, true);
            drawFittedCenteredUiText(context, second, centerX, control.y() + control.h() / 2 + refY(2), maxWidth, color, true);
        }
    }

    private void drawFinishedQuickControls(DrawContext context, int mouseX, int mouseY) {
        String[] labels = {"MÍN", "x5", "x10"};
        for (int i = 0; i < 3; i++) {
            int x = panelX + refX(1070 + i * 126);
            int y = panelY + refY(270);
            int w = refX(108);
            int h = refY(38);
            boolean hovered = inside(mouseX, mouseY, x, y, w, h);
            context.fill(x, y, x + w, y + h, hovered ? 0xFF7B174B : 0xCC250E28);
            outline(context, x, y, w, h, hovered ? 0xFFFFFFFF : 0xFFFFC94D);
            drawFittedCenteredUiText(context, labels[i], x + w / 2, y + h / 2 - refY(6), w - refX(10), WHITE, true);
        }
    }

    private void drawFinishedGameState(DrawContext context) {
        String game = safe(state.game(), "");
        if ("slot".equals(game)) {
            String[] raw = safe(state.message(), "BAYA|7|EMI").split("\\|");
            String[] values = {"BAYA", "7", "EMI"};
            if (raw.length >= 3) for (int i = 0; i < 3; i++) values[i] = compactLabel(raw[i], 8);
            int[] centers = {318, 525, 733};
            long elapsed = System.currentTimeMillis() - openedAt;
            String[] cycle = {"CEREZA", "BAYA", "CAMPANA", "ESTRELLA", "EMI", "JACKPOT"};
            boolean hasResult = raw.length >= 3;
            for (int i = 0; i < 3; i++) {
                long stopAt = 850L + i * 300L;
                boolean spinning = hasResult && elapsed < stopAt;
                String symbol = spinning ? cycle[(int)((elapsed / 85L + i * 2L) % cycle.length)] : values[i];
                int offset = spinning ? refY((int)((elapsed % 85L) * 54L / 85L)) : 0;
                int cx = panelX + refX(centers[i]);
                int reelLeft = cx - refX(86);
                context.enableScissor(reelLeft, panelY + refY(315), reelLeft + refX(172), panelY + refY(565));
                drawSlotSymbol(context, symbol, cx, panelY + refY(405) + offset, refX(86));
                if (spinning) drawSlotSymbol(context, cycle[(int)((elapsed / 85L + i * 2L + 1L) % cycle.length)],
                        cx, panelY + refY(405) + offset - refY(128), refX(86));
                context.disableScissor();
            }
        } else if ("dice".equals(game)) {
            int[] dice = diceValues();
            long elapsed = System.currentTimeMillis() - openedAt;
            boolean rolling = "result".equals(state.phase()) && hasDiceResult() && elapsed < 2600L;
            int leftValue = rolling ? 1 + (int)((elapsed / 95L) % 6L) : dice[0];
            int rightValue = rolling ? 1 + (int)((elapsed / 95L + 3L) % 6L) : dice[1];
            drawAnimatedDie(context, panelX + refX(337), panelY + refY(285), refX(175), leftValue,
                    0xFFF7FBFF, 0xFF43AFFF, elapsed, rolling, false);
            drawAnimatedDie(context, panelX + refX(565), panelY + refY(285), refX(175), rightValue,
                    0xFFFFF7FA, 0xFFFF526D, elapsed, rolling, true);
            drawFittedCenteredUiText(context, rolling ? "LANZANDO…" : "TOTAL " + (dice[0] + dice[1]), panelX + refX(540), panelY + refY(515),
                    refX(260), 0xFFFFD85B, true);
        } else if ("poker".equals(game)) {
            drawCardsInSlots(context, extractCards(state.tableState()), new int[] {265, 376, 487, 599, 709}, 292, 96, 160, 0L);
            drawCardsInSlots(context, extractCards(state.privateState()), new int[] {400, 530}, 533, 105, 170, 600L);
        } else if ("blackjack".equals(game)) {
            drawCardsInSlots(context, extractCards(state.tableState()), new int[] {275, 366, 457, 548, 639, 730}, 260, 78, 120, 0L);
            drawCardsInSlots(context, extractCards(state.privateState()), new int[] {241, 335, 429, 523, 617, 711}, 530, 82, 118, 420L);
        } else if ("claw".equals(game)) {
            drawInteractiveClaw(context);
        } else if ("pokemon_flip".equals(game)) {
            drawPokemonFlipSelections(context);
        }
    }

    private void drawInteractiveClaw(DrawContext context) {
        List<String> ids = state.itemIds() == null ? List.of() : state.itemIds();
        int[] laneCenters = {245, 390, 545, 690, 835};
        int laneCount = Math.min(ids.size(), laneCenters.length);
        int selected = Math.max(0, Math.min(Math.max(0, laneCount - 1), state.selectedIndex()));
        int clawX = panelX + refX(laneCenters[Math.min(selected, laneCenters.length - 1)]);
        long elapsed = System.currentTimeMillis() - openedAt;
        boolean caught = "caught".equals(state.phase()) && state.caughtIndex() >= 0;
        float animation = caught ? Math.min(1.0F, elapsed / 1450.0F) : 0.0F;
        float down = animation < 0.52F ? animation / 0.52F : Math.max(0.0F, 1.0F - (animation - 0.52F) / 0.48F);
        int clawY = panelY + refY(225 + Math.round(365.0F * down));

        for (int index = 0; index < laneCount; index++) {
            if (caught && index == state.caughtIndex() && animation > 0.52F) continue;
            Identifier id = Identifier.tryParse(ids.get(index));
            if (id == null || !Registries.ITEM.containsId(id)) continue;
            Item item = Registries.ITEM.get(id);
            if (item == net.minecraft.item.Items.AIR) continue;
            int centerX = panelX + refX(laneCenters[index]);
            int centerY = panelY + refY(682);
            drawLargeItem(context, new ItemStack(item), centerX, centerY, refX(index == selected ? 54 : 46));
            if (index == selected) {
                drawCircleOutline(context, centerX, panelY + refY(704), refX(42), 0xFFFFFFFF);
                drawFittedCenteredUiText(context, item.getName().getString(), centerX, panelY + refY(735),
                        refX(130), 0xFFFFE6A0, true);
            }
        }

        int railY = panelY + refY(188);
        context.fill(panelX + refX(145), railY, panelX + refX(920), railY + refY(5), 0xFFFFD166);
        context.fill(clawX - refX(2), railY, clawX + refX(2), clawY - refY(24), 0xFFB9B4C8);
        fillRoundedRect(context, clawX - refX(24), clawY - refY(28), refX(48), refY(35), refX(7), 0xFF8F2B6A);
        outline(context, clawX - refX(24), clawY - refY(28), refX(48), refY(35), 0xFFFFD166);
        context.fill(clawX - refX(4), clawY + refY(4), clawX + refX(4), clawY + refY(30), 0xFFFFD166);
        context.fill(clawX - refX(30), clawY + refY(24), clawX - refX(4), clawY + refY(30), 0xFFFFD166);
        context.fill(clawX + refX(4), clawY + refY(24), clawX + refX(30), clawY + refY(30), 0xFFFFD166);
        context.fill(clawX - refX(31), clawY + refY(24), clawX - refX(25), clawY + refY(50), 0xFFFF77B7);
        context.fill(clawX + refX(25), clawY + refY(24), clawX + refX(31), clawY + refY(50), 0xFFFF77B7);

        if (caught && state.caughtIndex() < laneCount && animation > 0.52F) {
            Identifier id = Identifier.tryParse(ids.get(state.caughtIndex()));
            if (id != null && Registries.ITEM.containsId(id)) {
                Item item = Registries.ITEM.get(id);
                drawLargeItem(context, new ItemStack(item), clawX, clawY + refY(60), refX(54));
            }
        }
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int centerX, int centerY, int size) {
        if (stack == null || stack.isEmpty()) return;
        float scale = Math.max(1.0F, size / 16.0F);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 120.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private void drawPokemonFlipSelections(DrawContext context) {
        List<CasinoNetworking.PokemonDisplay> displays = state.pokemonDisplays() == null ? List.of() : state.pokemonDisplays();
        int[] centersX = {220, 955};
        int[] frameX = {110, 850};
        int[] frameColors = {0xFF18B887, 0xFFE83C8A};
        for (int index = 0; index < 2; index++) {
            int x = panelX + refX(frameX[index]);
            int y = panelY + refY(265);
            int w = refX(225);
            int h = refY(360);
            if (index >= displays.size()) {
                drawCaptureBall(context, panelX + refX(centersX[index]), panelY + refY(430), refX(34));
                drawFittedCenteredUiText(context, "ESPERANDO", panelX + refX(centersX[index]), panelY + refY(535), refX(170), MUTED, true);
                continue;
            }
            CasinoNetworking.PokemonDisplay display = displays.get(index);
            int centerX = panelX + refX(centersX[index]);
            boolean rendered = !display.speciesId().isBlank() && drawCobblemonPortrait(context, display.speciesId(), centerX, panelY + refY(500), refX(78));
            if (!rendered) {
                drawCaptureBall(context, centerX, panelY + refY(420), refX(38));
                drawFittedCenteredUiText(context, compactLabel(display.speciesName(), 20), centerX,
                        panelY + refY(475), refX(180), WHITE, true);
            }
            drawFittedCenteredUiText(context, display.speciesName(), centerX, panelY + refY(545), refX(185), WHITE, true);
            drawFittedCenteredUiText(context, display.level() > 0 ? "NIVEL " + display.level() : "SIN SELECCIÓN", centerX,
                    panelY + refY(575), refX(165), 0xFFFFD85B, true);
            String status = display.ready() ? "CONFIRMADO" : "SELECCIONANDO";
            drawFittedCenteredUiText(context, status, centerX, panelY + refY(605), refX(175),
                    display.ready() ? 0xFFB9EFD2 : 0xFFFFE6A0, true);
            outline(context, x + refX(4), y + refY(4), w - refX(8), h - refY(8), frameColors[index]);
        }

        List<Integer> rounds = state.recentResults() == null ? List.of() : state.recentResults();
        int[] roundX = {390, 610, 830};
        for (int index = 0; index < roundX.length; index++) {
            int cx = panelX + refX(roundX[index]);
            int cy = panelY + refY(730);
            if (index >= rounds.size()) {
                drawFittedCenteredUiText(context, "?", cx, cy - refY(7), refX(60), MUTED, true);
                continue;
            }
            boolean cara = rounds.get(index) == 1;
            drawCoin(context, cx, cy, refX(34));
            drawFittedCenteredUiText(context, cara ? "C" : "S", cx, cy - refY(7), refX(44),
                    cara ? 0xFF17472F : 0xFFE83C8A, true);
        }
    }

    private boolean drawCobblemonPortrait(DrawContext context, String speciesId, int centerX, int bottomY, int scale) {
        return PokemonPortraitRenderer.draw(context, speciesId, centerX, bottomY, scale);
    }

    private void drawCardsInSlots(DrawContext context, List<String> cards, int[] sourceX, int sourceY, int sourceW, int sourceH, long baseDelay) {
        long elapsed = System.currentTimeMillis() - openedAt;
        for (int i = 0; i < cards.size() && i < sourceX.length; i++) {
            float progress = Math.min(1.0F, Math.max(0.0F, (elapsed - baseDelay - i * 125L) / 360.0F));
            if (progress <= 0.0F) continue;
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
            int x = panelX + refX(sourceX[i]);
            int y = panelY + refY(sourceY);
            int w = refX(sourceW);
            int h = refY(sourceH);
            String card = cards.get(i);
            float scale = 0.72F + eased * 0.28F;
            float angle = (1.0F - eased) * (i % 2 == 0 ? -12.0F : 12.0F);
            float slideY = -(1.0F - eased) * refY(190);
            context.getMatrices().push();
            context.getMatrices().translate(x + w / 2.0F, y + h / 2.0F + slideY, 0.0F);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            context.getMatrices().scale(scale, scale, 1.0F);
            drawCardFace(context, -w / 2, -h / 2, w, h, card);
            context.getMatrices().pop();
        }
    }

    private void drawCardFace(DrawContext context, int x, int y, int w, int h, String card) {
        context.fill(x + refX(4), y + refY(5), x + w + refX(4), y + h + refY(5), 0x99000000);
        fillRoundedRect(context, x, y, w, h, Math.max(3, refX(8)), 0xFFFFF8E9);
        outline(context, x, y, w, h, 0xFFFFD166);
        outline(context, x + refX(4), y + refY(4), w - refX(8), h - refY(8), 0xFFD6B76C);
        String rank = card.length() > 1 ? card.substring(0, card.length() - 1) : card;
        char suit = card.isEmpty() ? '♠' : card.charAt(card.length() - 1);
        int color = suit == '♥' || suit == '♦' ? 0xFFD32245 : 0xFF17131B;
        Text rankText = Text.literal(rank).setStyle(Style.EMPTY.withBold(true));
        context.drawTextWithShadow(textRenderer, rankText, x + refX(9), y + refY(9), color);
        int rankWidth = textRenderer.getWidth(rankText);
        context.getMatrices().push();
        context.getMatrices().translate(x + w - refX(9), y + h - refY(9), 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        context.drawTextWithShadow(textRenderer, rankText, 0, 0, color);
        context.getMatrices().pop();
        drawSuit(context, x + w / 2, y + h / 2, Math.max(7, Math.min(w, h) / 6), suit, color);
        if (rankWidth > w / 3) drawSuit(context, x + w / 2, y + h * 2 / 3, Math.max(5, Math.min(w, h) / 9), suit, color);
    }

    private void drawSuit(DrawContext context, int cx, int cy, int size, char suit, int color) {
        int small = Math.max(2, size / 2);
        if (suit == '♦') {
            for (int dy = -size; dy <= size; dy++) {
                int half = Math.max(1, size - Math.abs(dy));
                context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
            }
        } else if (suit == '♥') {
            drawCircle(context, cx - small / 2, cy - small / 2, small, color);
            drawCircle(context, cx + small / 2, cy - small / 2, small, color);
            for (int dy = 0; dy <= size; dy++) {
                int half = Math.max(1, size - dy);
                context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
            }
        } else if (suit == '♣') {
            drawCircle(context, cx, cy - small, small, color);
            drawCircle(context, cx - small, cy, small, color);
            drawCircle(context, cx + small, cy, small, color);
            context.fill(cx - Math.max(1, size / 4), cy, cx + Math.max(2, size / 4 + 1), cy + size + 1, color);
        } else {
            for (int dy = -size; dy <= 0; dy++) {
                int half = Math.max(1, size + dy);
                context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
            }
            drawCircle(context, cx - small / 2, cy, small, color);
            drawCircle(context, cx + small / 2, cy, small, color);
            context.fill(cx - Math.max(1, size / 4), cy, cx + Math.max(2, size / 4 + 1), cy + size + 1, color);
        }
    }

    private void drawSlotSymbol(DrawContext context, String symbol, int cx, int cy, int size) {
        String normalized = safe(symbol, "?").strip().toUpperCase();
        int symbolIndex;
        if (normalized.contains("CEREZA") || normalized.contains("CHERRY")
                || normalized.contains("BAYA") || normalized.contains("BERRY")) symbolIndex = 0;
        else if (normalized.contains("CAMPANA") || normalized.contains("BELL")) symbolIndex = 1;
        else if (normalized.contains("ESTRELLA") || normalized.contains("STAR")) symbolIndex = 2;
        else if (normalized.contains("JACKPOT") || normalized.contains("POKE") || normalized.contains("BALL")) symbolIndex = 3;
        else symbolIndex = 4; // EMI and unknown legacy values always remain graphical.
        int height = Math.max(1, Math.round(size * (SLOT_SYMBOL_CELL_H / (float) SLOT_SYMBOL_CELL_W)));
        context.drawTexture(CASINO_SLOT_SYMBOLS, cx - size / 2, cy - height / 2, size, height,
                (float)(symbolIndex * SLOT_SYMBOL_CELL_W), 0.0F,
                SLOT_SYMBOL_CELL_W, SLOT_SYMBOL_CELL_H, SLOT_SYMBOL_ATLAS_W, SLOT_SYMBOL_ATLAS_H);
    }

    private void drawDiamondStar(DrawContext context, int cx, int cy, int size, int color) {
        for (int dy = -size; dy <= size; dy++) {
            int half = Math.max(1, (size - Math.abs(dy)) / 2);
            context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
        for (int dx = -size; dx <= size; dx++) {
            int half = Math.max(1, (size - Math.abs(dx)) / 2);
            context.fill(cx + dx, cy - half, cx + dx + 1, cy + half + 1, color);
        }
    }

    private String diceResultSource() {
        return safe(state.tableState(), "") + " " + safe(state.message(), "");
    }

    private boolean hasDiceResult() {
        return Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)").matcher(diceResultSource()).find();
    }

    private void drawAnimatedDie(DrawContext context, int x, int y, int size, int value, int face, int pip,
                                 long elapsed, boolean rolling, boolean reverse) {
        float phase = Math.min(1.0F, elapsed / 2600.0F);
        float energy = 1.0F - phase;
        float bounce = rolling ? (float)Math.abs(Math.sin(elapsed / 105.0D)) * refY(58) * energy : 0.0F;
        float travel = rolling ? (float)Math.sin(elapsed / 135.0D) * refX(44) * energy * (reverse ? -1.0F : 1.0F) : 0.0F;
        float angle = rolling ? (elapsed * (reverse ? -0.39F : 0.39F)) % 360.0F : 0.0F;
        float pulse = rolling ? 0.88F + (float)Math.abs(Math.sin(elapsed / 115.0D)) * 0.18F : 1.0F;
        int shadowW = Math.round(size * (0.34F + phase * 0.18F));
        int shadowY = y + size + refY(12);
        context.fill(x + size / 2 - shadowW, shadowY - refY(4), x + size / 2 + shadowW, shadowY + refY(4), 0x55000000);
        context.getMatrices().push();
        context.getMatrices().translate(x + size / 2.0F + travel, y + size / 2.0F - bounce, 0.0F);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        context.getMatrices().scale(pulse, pulse, 1.0F);
        context.getMatrices().translate(-x - size / 2.0F, -y - size / 2.0F, 0.0F);
        drawDie(context, x, y, size, value, face, pip);
        context.getMatrices().pop();
    }

    private List<String> extractCards(String value) {
        List<String> cards = new ArrayList<>();
        Matcher match = Pattern.compile("(?:10|[2-9JQKA])[♠♥♦♣]").matcher(safe(value, ""));
        while (match.find()) cards.add(match.group());
        return cards;
    }

    private List<FinishedControl> finishedActionControls() {
        List<Action> actions = actions();
        String game = safe(state.game(), "");
        List<FinishedControl> result = new ArrayList<>();
        if ("dice".equals(game)) {
            int[] xs = {208, 443, 678};
            String[] order = {"under7", "exact7", "over7"};
            for (int i = 0; i < order.length; i++) {
                Action action = findAction(actions, order[i]);
                if (action != null) result.add(control(action, xs[i], 800, 184, 62));
            }
        } else if ("chip_exchange".equals(game)) {
            int[] xs = {145, 545};
            for (int i = 0; i < actions.size() && i < 2; i++) result.add(control(actions.get(i), xs[i], 785, 360, 75));
        } else if ("ticket_exchange".equals(game)) {
            if (!actions.isEmpty()) result.add(control(actions.get(0), 708, 805, 270, 85));
        } else if ("slot".equals(game)) {
            if (!actions.isEmpty()) result.add(control(actions.get(0), 245, 780, 280, 100));
        } else if ("blackjack".equals(game)) {
            int[] xs = {124, 338, 552, 766};
            for (int i = 0; i < actions.size() && i < 4; i++) result.add(control(actions.get(i), xs[i], 798, 174, 66));
        } else if ("poker".equals(game)) {
            int[] xs = {714, 824};
            for (int i = 0; i < actions.size() && i < 2; i++) result.add(control(actions.get(i), xs[i], 772, 104, 82));
        } else if ("claw".equals(game)) {
            if (actions.size() == 1) {
                result.add(control(actions.get(0), 610, 815, 250, 72));
            } else {
                int[] xs = {385, 565, 745};
                for (int i = 0; i < actions.size() && i < 3; i++) result.add(control(actions.get(i), xs[i], 815, 155, 72));
            }
        } else if ("pokemon_flip".equals(game)) {
            if (actions.size() == 1) {
                result.add(control(actions.get(0), 430, 845, 340, 66));
            } else {
                int[] xs = {120, 350, 580, 810};
                for (int i = 0; i < actions.size() && i < xs.length; i++) result.add(control(actions.get(i), xs[i], 845, 205, 66));
            }
        }
        return result;
    }

    private Action findAction(List<Action> actions, String actionId) {
        for (Action action : actions) if (action.action().equals(actionId)) return action;
        return null;
    }

    private FinishedControl control(Action action, int x, int y, int w, int h) {
        return new FinishedControl(action.label(), action.action(), panelX + refX(x), panelY + refY(y), refX(w), refY(h));
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
        context.enableScissor(panelX + refX(1125), panelY + refY(503), panelX + refX(1490), panelY + refY(589));
        drawScaledIntegratedWrapped(context, own, panelX + refX(1194), panelY + refY(520),
                refX(280), WHITE, panelY + refY(540), 0.82F);
        if (selected.isBlank()) {
            drawScaledIntegratedWrapped(context, "Selecciona una casilla en el tapete",
                    panelX + refX(1194), panelY + refY(544), refX(280), MUTED, panelY + refY(568), 0.74F);
        }
        context.disableScissor();

        List<String> players = state.players() == null ? List.of() : state.players();
        renderPlayerCount(context, players.size());
        context.enableScissor(panelX + refX(1124), panelY + refY(666), panelX + refX(1495), panelY + refY(790));
        drawPlayers(context, contentTop + sidePy(365), contentTop + sidePy(449));
        context.disableScissor();

        context.enableScissor(panelX + refX(1127), panelY + refY(842), panelX + refX(1418), panelY + refY(929));
        drawScaledIntegratedWrapped(context, safe(state.tableState(), "Mesa lista para una nueva ronda."),
                panelX + refX(1194), panelY + refY(865), refX(218), WHITE, panelY + refY(918), 0.72F);
        context.disableScissor();
    }

    private void renderHeaderBalance(DrawContext context) {
        // The HD header already contains the coin and an empty burgundy balance capsule.
        int centerX = panelX + refX(1262);
        int y = panelY + refY(82);
        String value = state.balance() + " Michicoins";
        context.enableScissor(panelX + refX(1182), panelY + refY(62), panelX + refX(1340), panelY + refY(111));
        drawFittedCenteredUiText(context, value, centerX, y, refX(145), 0xFFFFDF62, false);
        context.disableScissor();
    }

    private void renderRoundState(DrawContext context) {
        int x = panelX + refX(1126);
        int y = panelY + refY(196);
        int badgeW = refX(127);
        drawFittedCenteredUiText(context, phaseLabel(), x + badgeW / 2, y + refY(9),
                badgeW - refX(12), isBettingPhase() ? 0xFF9AF2B6 : 0xFFFFDB68, false);
        String timer = timerOnly();
        if (!timer.isBlank()) {
            int timerCenter = panelX + refX(1374);
            drawFittedCenteredUiText(context, "Inicia en:", timerCenter, y, refX(92), 0xFFF7F0E4, false);
            drawFittedCenteredUiText(context, timer, timerCenter, y + refY(18), refX(92), 0xFFFFE9A1, false);
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
        int x = panelX + refX(1424);
        int y = panelY + refY(600);
        int w = refX(52);
        String value = count + "/" + ROULETTE_DISPLAY_CAPACITY;
        drawFittedCenteredUiText(context, value, x + w / 2, y + refY(8),
                w - refX(6), 0xFFFFD85B, false);
    }

    private Text casinoText(String value, boolean bold) {
        return Text.literal(value).setStyle(Style.EMPTY.withFont(CASINO_FONT).withBold(bold));
    }

    private void drawReadableText(DrawContext context, Text text, int x, int y, int color) {
        int outline = 0xE0100716;
        context.drawText(textRenderer, text, x - 1, y, outline, false);
        context.drawText(textRenderer, text, x + 1, y, outline, false);
        context.drawText(textRenderer, text, x, y - 1, outline, false);
        context.drawText(textRenderer, text, x, y + 1, outline, false);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawReadableText(DrawContext context, OrderedText text, int x, int y, int color) {
        int outline = 0xE0100716;
        context.drawText(textRenderer, text, x - 1, y, outline, false);
        context.drawText(textRenderer, text, x + 1, y, outline, false);
        context.drawText(textRenderer, text, x, y - 1, outline, false);
        context.drawText(textRenderer, text, x, y + 1, outline, false);
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    private void drawUiText(DrawContext context, String value, int x, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        drawReadableText(context, text, x, y, color);
    }

    private void drawCenteredUiText(DrawContext context, String value, int centerX, int y, int color, boolean bold) {
        Text text = casinoText(value, bold);
        drawReadableText(context, text, centerX - textRenderer.getWidth(text) / 2, y, color);
    }

    private void drawFittedCenteredUiText(DrawContext context, String value, int centerX, int y,
                                          int maxWidth, int color, boolean bold) {
        Text text = casinoText(value, bold);
        int textWidth = Math.max(1, textRenderer.getWidth(text));
        float scale = Math.min(1.0F, maxWidth / (float)textWidth);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        drawReadableText(context, text, -textWidth / 2, 0, color);
        context.getMatrices().pop();
    }

    private int drawIntegratedWrapped(DrawContext context, String value, int x, int y, int width, int color, int maxY) {
        if (value == null || value.isBlank()) return y;
        for (var line : textRenderer.wrapLines(casinoText(value, false), width)) {
            if (y > maxY) break;
            drawReadableText(context, line, x, y, color);
            y += 11;
        }
        return y;
    }

    private void drawScaledIntegratedWrapped(DrawContext context, String value, int x, int y,
                                             int width, int color, int maxY, float scale) {
        if (value == null || value.isBlank()) return;
        int wrapWidth = Math.max(1, Math.round(width / scale));
        int localMaxY = Math.max(0, Math.round((maxY - y) / scale));
        int localY = 0;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        for (var line : textRenderer.wrapLines(casinoText(value, false), wrapWidth)) {
            if (localY + 9 > localMaxY) break;
            drawReadableText(context, line, 0, localY, color);
            localY += 11;
        }
        context.getMatrices().pop();
    }

    private void drawRouletteWheel(DrawContext context) {
        int size = Math.max(180, refX(438));
        float numberScale = Math.max(0.72F, Math.min(0.86F, size / 438.0F * 0.92F));
        int numberRadius = Math.round(size * 0.286F);

        context.getMatrices().push();
        context.getMatrices().translate(wheelCx, wheelCy, 0.0F);
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
            drawReadableText(context, numberText, -textRenderer.getWidth(numberText) / 2, -4, WHITE);
            context.getMatrices().pop();
        }
        context.getMatrices().pop();

        // The wheel texture owns its center art; a second static medallion caused a duplicated logo.
        drawRouletteBall(context, size);
    }

    private void drawRouletteBall(DrawContext context, int wheelSize) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        int result = rouletteResultNumber();
        boolean settling = "result".equals(state.phase()) && result >= 0;
        double t = settling ? Math.min(1.0D, elapsed / 3200.0D) : 0.0D;
        double eased = 1.0D - Math.pow(1.0D - t, 4.0D);
        double freeAngle = -Math.PI / 2.0D - elapsed * 0.0047D;
        int resultIndex = 0;
        if (result >= 0) {
            for (int i = 0; i < ROULETTE_WHEEL.length; i++) {
                if (ROULETTE_WHEEL[i] == result) {
                    resultIndex = i;
                    break;
                }
            }
        }
        double resultAngle = TAU * resultIndex / ROULETTE_WHEEL.length - Math.PI / 2.0D;
        double targetAngle = resultAngle - TAU * 3.0D;
        double angle = freeAngle * (1.0D - eased) + targetAngle * eased;
        double radius = wheelSize * 0.370D;
        int bx = wheelCx + (int)Math.round(Math.cos(angle) * radius);
        int by = wheelCy + (int)Math.round(Math.sin(angle) * radius);
        int ballRadius = Math.max(3, refX(8));
        drawCircle(context, bx + 1, by + 2, ballRadius + 1, 0x66000000);
        drawCircle(context, bx, by, ballRadius, 0xFFF8F7EE);
        drawCircleOutline(context, bx, by, ballRadius, 0xFFFFE8A3);
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
            drawReadableText(context, casinoText("Esperando apuestas…", false), sideX + sidePx(34), y, MUTED);
            return;
        }
        int shown = 0;
        for (String player : players) {
            if (y + 14 > maxY || shown >= 5) break;
            drawCircle(context, sideX + sidePx(29), y + 5, Math.max(3, sidePx(5)), 0xFF485078);
            String clipped = textRenderer.trimToWidth(player, sideW - sidePx(58));
            drawReadableText(context, casinoText(clipped, false), sideX + sidePx(41), y + 1, WHITE);
            y += Math.max(13, sidePy(18));
            shown++;
        }
        if (players.size() > shown && y <= maxY) {
            drawReadableText(context, casinoText("+" + (players.size() - shown) + " más", false), sideX + sidePx(41), y, MUTED);
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

    private CasinoTheme casinoTheme() {
        return switch (safe(state.game(), "slot")) {
            case "chip_exchange" -> new CasinoTheme("CAMBIO DE FICHAS", 0xFF0B443F, 0xFF37C9B2, 0xFFFFD166, 0xFF102A2C);
            case "ticket_exchange" -> new CasinoTheme("TICKETS DEL CASINO", 0xFF51203A, 0xFFFF77B7, 0xFFFFD166, 0xFF2A1425);
            case "dice" -> new CasinoTheme("MESA DE DADOS", 0xFF163F67, 0xFF55B7FF, 0xFFFFD166, 0xFF111F38);
            case "blackjack" -> new CasinoTheme("BLACKJACK", 0xFF17472F, 0xFF75D98B, 0xFFFFD166, 0xFF11291E);
            case "poker" -> new CasinoTheme("POKER EMI", 0xFF5A1727, 0xFFFF596D, 0xFFFFD166, 0xFF29101A);
            case "claw" -> new CasinoTheme("GARRA DE PELUCHES", 0xFF3B1750, 0xFFFF77B7, 0xFFFFD166, 0xFF15152A);
            case "pokemon_flip" -> new CasinoTheme("CARA O SELLO POKÉMON", 0xFF184E3D, 0xFFEA3C83, 0xFFFFD166, 0xFF101C29);
            default -> new CasinoTheme("TRAGAMONEDAS EMI", 0xFF4B184F, 0xFFFF73E2, 0xFFFFD166, 0xFF28112D);
        };
    }

    /**
     * Keeps presentation effects local to the current client. The server remains the
     * only authority for results, balances and rewards; this method only reacts to
     * the immutable state it already sent.
     */
    private void updateCasinoSounds() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        String game = safe(state.game(), "");

        if ("roulette".equals(game) && "result".equals(state.phase()) && rouletteResultNumber() >= 0) {
            playTimedTicks(elapsed, 3200L, 125L, 0.72F, 0.012F, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value());
            finishSoundAfter(elapsed, 3200L);
        } else if ("dice".equals(game) && "result".equals(state.phase()) && hasDiceResult()) {
            playTimedTicks(elapsed, 2600L, 170L, 0.82F, 0.018F, SoundEvents.BLOCK_STONE_HIT);
            finishSoundAfter(elapsed, 2600L);
        } else if ("slot".equals(game) && safe(state.message(), "").split("\\|").length >= 3) {
            playTimedTicks(elapsed, 1450L, 95L, 0.82F, 0.014F, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value());
            int stopped = elapsed >= 1450L ? 3 : elapsed >= 1150L ? 2 : elapsed >= 850L ? 1 : 0;
            if (stopped > presentation.slotStops) {
                presentation.slotStops = stopped;
                playLocal(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.92F + stopped * 0.08F);
            }
            finishSoundAfter(elapsed, 1750L);
        } else if ("blackjack".equals(game) || "poker".equals(game)) {
            int cardCount = extractCards(state.tableState()).size() + extractCards(state.privateState()).size();
            int audibleCards = Math.min(cardCount, Math.max(0, (int)(elapsed / 145L) + 1));
            if (presentation.cardSounds < audibleCards) {
                presentation.cardSounds++;
                playLocal(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.92F + (presentation.cardSounds % 3) * 0.05F);
            }
            if ("result".equals(state.phase())) finishSoundAfter(elapsed, Math.max(900L, cardCount * 145L + 420L));
        }
    }

    private void playTimedTicks(long elapsed, long duration, long interval, float basePitch, float pitchStep, SoundEvent sound) {
        if (elapsed >= duration) return;
        long step = elapsed / interval;
        if (step == presentation.lastTimedStep) return;
        presentation.lastTimedStep = step;
        playLocal(sound, Math.min(1.7F, basePitch + step * pitchStep));
    }

    private void finishSoundAfter(long elapsed, long duration) {
        if (elapsed < duration || presentation.resultSoundPlayed) return;
        presentation.resultSoundPlayed = true;
        String feedback = (safe(state.message(), "") + ' ' + safe(state.privateState(), "")).toLowerCase(java.util.Locale.ROOT);
        boolean win = feedback.contains("ganaste") || feedback.contains("premio") || feedback.contains("jackpot");
        playLocal(win ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                win ? 1.05F : 0.84F);
    }

    private void playActionSound(String action) {
        switch (safe(action, "")) {
            case "spin" -> playLocal(SoundEvents.BLOCK_DISPENSER_LAUNCH, 1.02F);
            case "hit" -> playLocal(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.08F);
            case "buy_ticket" -> playLocal(SoundEvents.BLOCK_DISPENSER_DISPENSE, 1.06F);
            case "claw_drop" -> playLocal(SoundEvents.BLOCK_PISTON_EXTEND, 1.04F);
            case "claw_left", "claw_right" -> playLocal(SoundEvents.BLOCK_CHAIN_HIT, 1.15F);
            case "claw_reset" -> playLocal(SoundEvents.UI_BUTTON_CLICK.value(), 1.08F);
            case "ready" -> playLocal(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.12F);
            case "buy", "sell", "join", "under7", "exact7", "over7" ->
                    playLocal(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.12F);
            default -> playLocal(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
        }
    }

    private void playLocal(SoundEvent sound, float pitch) {
        if (client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }

    private void drawCasinoFrame(DrawContext context, CasinoTheme theme) {
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, theme.deep());
        outline(context, panelX, panelY, panelW, panelH, theme.gold());
        outline(context, panelX + 3, panelY + 3, panelW - 6, panelH - 6, theme.accent());
        context.fill(panelX + 4, panelY + 4, panelX + panelW - 4, panelY + 62, 0xFF3B102C);
        context.fill(panelX + 4, panelY + 58, panelX + panelW - 4, panelY + 62, theme.gold());
        drawCasinoChip(context, panelX + 31, panelY + 31, 16, theme.accent());
        drawCasinoChip(context, panelX + panelW / 2, panelY + 31, 18, 0xFFE83E68);
        drawFittedCenteredUiText(context, theme.title(), panelX + 190, panelY + 17, 270, WHITE, true);
        drawFittedCenteredUiText(context, "EMI CASINO", panelX + panelW / 2, panelY + 17, 180, theme.gold(), true);
        drawFittedCenteredUiText(context, "MESA MULTIJUGADOR · SERVIDOR AUTORITATIVO",
                panelX + 190, panelY + 37, 310, 0xFFB9EFD2, false);
        String balance = state.balance() + " Michicoins";
        drawFittedCenteredUiText(context, balance, panelX + panelW - 190, panelY + 27, 190, theme.gold(), false);
    }

    private void drawGameShowcase(DrawContext context, CasinoTheme theme) {
        int x = gameX + 14;
        int y = contentTop + 196;
        int w = gameW - 28;
        int h = Math.max(100, panelY + panelH - 104 - y);
        context.fill(x, y, x + w, y + h, theme.deep());
        outline(context, x, y, w, h, theme.gold());
        outline(context, x + 3, y + 3, w - 6, h - 6, theme.accent());
        switch (safe(state.game(), "slot")) {
            case "chip_exchange" -> drawChipExchangeShowcase(context, theme, x, y, w, h);
            case "ticket_exchange" -> drawTicketShowcase(context, theme, x, y, w, h);
            case "dice" -> drawDiceShowcase(context, theme, x, y, w, h);
            case "blackjack" -> drawCardShowcase(context, theme, x, y, w, h, false);
            case "poker" -> drawCardShowcase(context, theme, x, y, w, h, true);
            default -> drawSlotShowcase(context, theme, x, y, w, h);
        }
    }

    private void drawSlotShowcase(DrawContext context, CasinoTheme theme, int x, int y, int w, int h) {
        drawFittedCenteredUiText(context, "PREMIO EMI", x + w / 2, y + 15, w - 30, theme.gold(), true);
        String[] raw = safe(state.message(), "BAYA|7|EMI").split("\\|");
        String[] symbols = {"BAYA", "7", "EMI"};
        if (raw.length >= 3) for (int i = 0; i < 3; i++) symbols[i] = compactLabel(raw[i], 8);
        int reelW = Math.max(62, Math.min(104, (w - 64) / 3));
        int gap = 12;
        int start = x + (w - reelW * 3 - gap * 2) / 2;
        int reelY = y + 43;
        int reelH = Math.max(60, h - 76);
        for (int i = 0; i < 3; i++) {
            int rx = start + i * (reelW + gap);
            context.fill(rx, reelY, rx + reelW, reelY + reelH, 0xFFF8E8D0);
            outline(context, rx, reelY, reelW, reelH, theme.gold());
            outline(context, rx + 4, reelY + 4, reelW - 8, reelH - 8, theme.accent());
            drawFittedCenteredUiText(context, symbols[i], rx + reelW / 2, reelY + reelH / 2 - 5,
                    reelW - 12, i == 1 ? 0xFFD02B43 : 0xFF3B183B, true);
        }
    }

    private void drawChipExchangeShowcase(DrawContext context, CasinoTheme theme, int x, int y, int w, int h) {
        drawFittedCenteredUiText(context, "FICHAS  ⇄  MICHICOINS", x + w / 2, y + 15, w - 24, theme.gold(), true);
        int cy = y + h / 2 + 12;
        for (int row = 0; row < 3; row++) for (int col = 0; col <= row; col++)
            drawCasinoChip(context, x + w / 4 + (col - row / 2) * 20, cy - row * 15, 13, row % 2 == 0 ? theme.accent() : 0xFFE84265);
        for (int row = 0; row < 3; row++) for (int col = 0; col <= row; col++)
            drawCoin(context, x + w * 3 / 4 + (col - row / 2) * 18, cy - row * 14, 12);
        drawFittedCenteredUiText(context, "COMPRA O CANJEA", x + w / 2, cy - 4, w / 3, WHITE, false);
    }

    private void drawTicketShowcase(DrawContext context, CasinoTheme theme, int x, int y, int w, int h) {
        int tw = Math.min(310, w - 70);
        int th = Math.min(126, h - 50);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - th) / 2;
        context.fill(tx, ty, tx + tw, ty + th, 0xFFFFE9C9);
        outline(context, tx, ty, tw, th, theme.gold());
        outline(context, tx + 6, ty + 6, tw - 12, th - 12, theme.accent());
        drawCasinoChip(context, tx + 35, ty + th / 2, 18, theme.accent());
        drawCasinoChip(context, tx + tw - 35, ty + th / 2, 18, theme.accent());
        drawFittedCenteredUiText(context, "TICKET NORMAL", tx + tw / 2, ty + 35, tw - 100, 0xFF59163B, true);
        drawFittedCenteredUiText(context, "ÚNICO DISPONIBLE", tx + tw / 2, ty + 61, tw - 100, 0xFF8E315C, false);
    }

    private void drawDiceShowcase(DrawContext context, CasinoTheme theme, int x, int y, int w, int h) {
        int[] dice = diceValues();
        int size = Math.min(94, Math.max(58, h - 58));
        int gap = 24;
        int start = x + (w - size * 2 - gap) / 2;
        int dy = y + (h - size) / 2 + 7;
        drawDie(context, start, dy, size, dice[0], 0xFFF5F7FF, theme.accent());
        drawDie(context, start + size + gap, dy, size, dice[1], 0xFFF5F7FF, 0xFFE84D65);
        drawFittedCenteredUiText(context, "TOTAL " + (dice[0] + dice[1]), x + w / 2, y + 15, w - 30, theme.gold(), true);
    }

    private void drawDie(DrawContext context, int x, int y, int size, int value, int face, int pip) {
        int corner = Math.max(4, size / 10);
        fillRoundedRect(context, x + Math.max(2, size / 24), y + Math.max(3, size / 20), size, size, corner, 0x88000000);
        fillRoundedRect(context, x, y, size, size, corner, 0xFFFFD166);
        fillRoundedRect(context, x + Math.max(2, size / 40), y + Math.max(2, size / 40),
                size - Math.max(4, size / 20), size - Math.max(4, size / 20), Math.max(3, corner - 2), face);
        context.fill(x + size / 8, y + size / 10, x + size * 7 / 8, y + size / 10 + Math.max(2, size / 35), 0x66FFFFFF);
        int r = Math.max(3, size / 14);
        int left = x + size / 4, center = x + size / 2, right = x + size * 3 / 4;
        int top = y + size / 4, middle = y + size / 2, bottom = y + size * 3 / 4;
        if (value != 1) { drawCircle(context, left, top, r, pip); drawCircle(context, right, bottom, r, pip); }
        if (value >= 4) { drawCircle(context, right, top, r, pip); drawCircle(context, left, bottom, r, pip); }
        if (value == 6) { drawCircle(context, left, middle, r, pip); drawCircle(context, right, middle, r, pip); }
        if ((value & 1) == 1) drawCircle(context, center, middle, r, pip);
    }

    private void drawCardShowcase(DrawContext context, CasinoTheme theme, int x, int y, int w, int h, boolean poker) {
        List<String> cards = visibleCards();
        int count = poker ? 5 : 4;
        int cardW = Math.max(42, Math.min(70, (w - 54) / count));
        int cardH = Math.min(108, Math.max(68, h - 60));
        int gap = 8;
        int start = x + (w - cardW * count - gap * (count - 1)) / 2;
        int cardY = y + (h - cardH) / 2 + 8;
        drawFittedCenteredUiText(context, poker ? "CARTAS DE LA MESA" : "MANO DEL CRUPIER",
                x + w / 2, y + 14, w - 24, theme.gold(), true);
        for (int i = 0; i < count; i++) {
            int cx = start + i * (cardW + gap);
            String card = i < cards.size() ? cards.get(i) : "?";
            context.fill(cx, cardY, cx + cardW, cardY + cardH, 0xFFF8F1E5);
            outline(context, cx, cardY, cardW, cardH, theme.gold());
            outline(context, cx + 3, cardY + 3, cardW - 6, cardH - 6, theme.accent());
            int color = card.contains("♥") || card.contains("♦") ? 0xFFC9273D : 0xFF1C1720;
            drawFittedCenteredUiText(context, card, cx + cardW / 2, cardY + cardH / 2 - 5, cardW - 10, color, true);
        }
    }

    private int[] diceValues() {
        Matcher match = Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)").matcher(diceResultSource());
        if (match.find()) return new int[] {Math.max(1, Math.min(6, Integer.parseInt(match.group(1)))), Math.max(1, Math.min(6, Integer.parseInt(match.group(2))))};
        return new int[] {1 + (int)Math.floorMod(state.roundId(), 6L), 1 + (int)Math.floorMod(state.roundId() / 3L + 2L, 6L)};
    }

    private List<String> visibleCards() {
        List<String> cards = new ArrayList<>();
        Matcher match = Pattern.compile("(?:10|[2-9JQKA])[♠♥♦♣]").matcher(safe(state.tableState(), "") + " " + safe(state.privateState(), ""));
        while (match.find()) cards.add(match.group());
        return cards;
    }

    private String compactLabel(String value, int max) {
        String clean = safe(value, "?").strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private void renderGeneric(DrawContext context, CasinoTheme theme) {
        context.fill(gameX, contentTop, gameX + gameW, panelY + panelH - 24, theme.panel());
        context.fill(sideX, contentTop, sideX + sideW, panelY + panelH - 24, theme.deep());
        outline(context, gameX, contentTop, gameW, panelY + panelH - 24 - contentTop, theme.gold());
        outline(context, sideX, contentTop, sideW, panelY + panelH - 24 - contentTop, theme.gold());

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

        drawGameShowcase(context, theme);

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
        if (button == 0 && !isRoulette() && finishedAsset() != null) {
            if (inside(mouseX, mouseY, panelX + refX(1380), panelY + refY(35), refX(120), refY(90))) {
                close();
                return true;
            }
            if (amountField != null) {
                for (int i = 0; i < 3; i++) {
                    if (inside(mouseX, mouseY, panelX + refX(1070 + i * 126), panelY + refY(270), refX(108), refY(38))) {
                        setQuickAmount(i == 0 ? 1 : i == 1 ? 5 : 10);
                        return true;
                    }
                }
            }
            for (FinishedControl control : finishedActionControls()) {
                if (control.contains(mouseX, mouseY)) {
                    send(control.action());
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
        } else if ("claw".equals(game)) {
            if ("caught".equals(phase)) result.add(new Action("Otra ronda", "claw_reset"));
            else if (!"blocked".equals(phase)) {
                result.add(new Action("← Izquierda", "claw_left"));
                result.add(new Action("Bajar garra", "claw_drop"));
                result.add(new Action("Derecha →", "claw_right"));
            }
        } else if ("pokemon_flip".equals(game)) {
            if ("lobby".equals(phase)) result.add(new Action("Unirte", "join"));
            else if ("selecting".equals(phase)) {
                result.add(new Action("Anterior", "previous"));
                result.add(new Action("Confirmar", "ready"));
                result.add(new Action("Siguiente", "next"));
                result.add(new Action("Cancelar", "cancel"));
            }
        } else if ("dice".equals(game) && isBettingPhase()) {
            result.add(new Action("Menos de 7", "under7"));
            result.add(new Action("Exactamente 7", "exact7"));
            result.add(new Action("Más de 7", "over7"));
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
        playActionSound(action);
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
            drawReadableText(context, line, x, y, color);
            y += 13;
        }
        return y;
    }

    private void fillRoundedRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        context.fill(x + r, y, x + w - r, y + h, color);
        context.fill(x, y + r, x + w, y + h - r, color);
        for (int dy = 0; dy < r; dy++) {
            int dx = (int)Math.floor(Math.sqrt(Math.max(0, r * r - (r - dy) * (r - dy))));
            context.fill(x + r - dx, y + dy, x + w - r + dx, y + dy + 1, color);
            context.fill(x + r - dx, y + h - dy - 1, x + w - r + dx, y + h - dy, color);
        }
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
    @Override public void close() {
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), 0.82F);
        if (client != null) client.setScreen(parent);
    }

    static final class PresentationState {
        private final String signature;
        private final long startedAt;
        private long lastTimedStep = -1L;
        private int slotStops;
        private int cardSounds;
        private boolean resultSoundPlayed;

        private PresentationState(String signature, long startedAt) {
            this.signature = signature;
            this.startedAt = startedAt;
        }
    }

    private record CasinoTheme(String title, int panel, int accent, int gold, int deep) { }
    private record FinishedControl(String label, String action, int x, int y, int w, int h) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
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
