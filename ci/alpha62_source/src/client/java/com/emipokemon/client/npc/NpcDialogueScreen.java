package com.emipokemon.client.npc;

import com.emipokemon.npc.NpcNetworking.DialogueState;
import com.emipokemon.npc.NpcNetworking.StartBattlePayload;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

final class NpcDialogueScreen extends Screen {
    private static final Gson GSON = new Gson();
    private final Screen parent;
    private final DialogueState state;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    NpcDialogueScreen(Screen parent, String json) {
        super(Text.literal("Diálogo NPC"));
        this.parent = parent;
        this.state = GSON.fromJson(json, DialogueState.class);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(560, width - 30);
        panelHeight = Math.min(285, height - 30);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int buttonY = panelY + panelHeight - 38;
        if (state.hasTeam()) {
            addDrawableChild(new AdminButtonWidget(panelX + 18, buttonY, 150, 24,
                    Text.literal("Luchar"), () -> {
                ClientPlayNetworking.send(new StartBattlePayload(state.id()));
                close();
            }));
        }
        addDrawableChild(new AdminButtonWidget(panelX + panelWidth - 118, buttonY, 100, 24,
                Text.literal("Cerrar"), this::close));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x86000000);
        drawPanel(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX + 6, panelY + 8, panelX + panelWidth + 6, panelY + panelHeight + 8, 0x82000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF5140C19);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 5, 0xFFFF9DDE);
        context.fill(panelX, panelY + 5, panelX + panelWidth, panelY + 8, 0xFF75418E);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(state.name()),
                panelX + panelWidth / 2, panelY + 20, 0xFFFFD8F1);
        context.fill(panelX + 16, panelY + 42, panelX + panelWidth - 16, panelY + panelHeight - 54, 0x8E28152F);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(state.dialogue()), panelWidth - 56);
        int y = panelY + 57;
        for (OrderedText line : lines) {
            if (y > panelY + panelHeight - 104) break;
            context.drawTextWithShadow(textRenderer, line, panelX + 28, y, 0xFFFFEAF8);
            y += 13;
        }
        String rewardStatus = state.rewardClaimed() && !state.rewardRepeatable()
                ? "Recompensa ya reclamada"
                : "Recompensa: " + state.rewards() + (state.rewardRepeatable() ? " (repetible)" : " (una vez)");
        context.drawTextWithShadow(textRenderer, Text.literal(rewardStatus), panelX + 28,
                panelY + panelHeight - 87, state.rewardClaimed() ? 0xFF9B8796 : 0xFFFFD56A);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
