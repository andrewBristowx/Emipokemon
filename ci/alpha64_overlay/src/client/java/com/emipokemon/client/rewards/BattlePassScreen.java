package com.emipokemon.client.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.registry.ModRegistries;
import com.emipokemon.rewards.BattlePassSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

final class BattlePassScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of(Emipokemon.MOD_ID, "textures/gui/battle_pass.png");
    private static final int REF_W = 1536;
    private static final int REF_H = 1024;
    private final Screen parent;
    private final BattlePassSnapshot snapshot;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    BattlePassScreen(Screen parent, BattlePassSnapshot snapshot) {
        super(Text.literal("Pase infinito de Emi"));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    Screen parent() { return parent; }

    @Override
    protected void init() {
        int availableW = Math.min(REF_W, Math.max(1, width - 18));
        int availableH = Math.min(REF_H, Math.max(1, height - 18));
        if (availableW / (float) availableH > REF_W / (float) REF_H) {
            panelH = availableH;
            panelW = Math.round(panelH * REF_W / (float) REF_H);
        } else {
            panelW = availableW;
            panelH = Math.round(panelW * REF_H / (float) REF_W);
        }
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        addDrawableChild(new PassButtonWidget(panelX + rx(1438), panelY + ry(28), rx(70), ry(62),
                Text.literal("×"), button -> close()));
        addDrawableChild(new PassButtonWidget(panelX + rx(25), panelY + ry(520), rx(75), ry(70),
                Text.literal("‹"), button -> BattlePassClient.request("page", Math.max(0, snapshot.page - 1))));
        addDrawableChild(new PassButtonWidget(panelX + rx(1435), panelY + ry(520), rx(75), ry(70),
                Text.literal("›"), button -> BattlePassClient.request("page", snapshot.page + 1)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD00A0710);
        context.drawTexture(BACKGROUND, panelX, panelY, panelW, panelH, 0, 0, REF_W, REF_H, REF_W, REF_H);
        drawHeader(context);
        drawTrack(context, snapshot.free, false, mouseX, mouseY);
        drawTrack(context, snapshot.premiumTrack, true, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHeader(DrawContext context) {
        drawCentered(context, "PASE INFINITO DE EMI", 760, 104, 0xFFFFE4F4);
        drawCentered(context, "NIVEL " + snapshot.level, 760, 143, 0xFFFFD36A);
        drawCentered(context, snapshot.playerName, 134, 120, 0xFFFFFFFF);
        drawCentered(context, "Nv. " + snapshot.level, 134, 156, 0xFFFFD36A);
        long span = Math.max(1L, snapshot.nextLevelXp - snapshot.levelStartXp);
        double fraction = MathHelper.clamp((snapshot.experience - snapshot.levelStartXp) / (double) span, 0.0D, 1.0D);
        int left = panelX + rx(448);
        int right = panelX + rx(1073);
        int top = panelY + ry(178);
        context.fill(left, top, right, top + Math.max(4, ry(13)), 0xCC160C25);
        context.fill(left + 1, top + 1, left + Math.max(2, Math.round((right - left - 2) * (float) fraction)),
                top + Math.max(3, ry(12)), 0xFFFF6EB5);
        drawCentered(context, (snapshot.experience - snapshot.levelStartXp) + " / " + span + " XP", 760, 176, 0xFFFFFFFF);
        drawCentered(context, "GRATIS", 135, 421, 0xFFFFDBEE);
        drawCentered(context, snapshot.premium ? "VIP ACTIVO" : "VIP BLOQUEADO", 135, 735,
                snapshot.premium ? 0xFFFFD36A : 0xFFB6A8B5);
        drawCentered(context, "Tiradas guardadas: " + snapshot.emiRolls, 760, 222, 0xFFFFD36A);
        if (snapshot.message != null && !snapshot.message.isBlank()) drawCentered(context, snapshot.message, 760, 958, 0xFFFFFFFF);
    }

    private void drawTrack(DrawContext context, List<BattlePassSnapshot.RewardSlot> slots, boolean premium,
                           int mouseX, int mouseY) {
        int[] centers = {278, 432, 586, 740, 894, 1048, 1202, 1356};
        int centerY = premium ? 755 : 408;
        int iconY = premium ? 730 : 383;
        for (int index = 0; index < Math.min(centers.length, slots.size()); index++) {
            BattlePassSnapshot.RewardSlot slot = slots.get(index);
            int cx = panelX + rx(centers[index]);
            int cy = panelY + ry(centerY);
            int cardX = panelX + rx(centers[index] - 67);
            int cardY = panelY + ry(premium ? 630 : 286);
            int cardW = rx(134);
            int cardH = ry(248);
            boolean hovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;
            if (!slot.unlocked || premium && !snapshot.premium) context.fill(cardX + 2, cardY + 2, cardX + cardW - 2, cardY + cardH - 2, 0x77000000);
            if (hovered && slot.claimable && (!premium || snapshot.premium))
                context.fill(cardX + 3, cardY + 3, cardX + cardW - 3, cardY + cardH - 3, 0x42FFFFFF);
            drawCenteredPx(context, "NIVEL " + slot.level, cx, panelY + ry(premium ? 655 : 311), 0xFFFFFFFF);
            if (slot.amount > 0) {
                drawLargeItem(context, new ItemStack(ModRegistries.EMI_SPECIAL_BANNER_TICKET), cx,
                        panelY + ry(iconY), rx(46));
                drawCenteredPx(context, "×" + slot.amount, cx, panelY + ry(premium ? 792 : 445), 0xFFFFD36A);
                String status = slot.claimed ? "RECLAMADO" : slot.unlocked && (!premium || snapshot.premium) ? "CLIC PARA RECLAMAR" : "BLOQUEADO";
                drawCenteredPx(context, status, cx, panelY + ry(premium ? 842 : 495),
                        slot.claimed ? 0xFF9CE3B0 : slot.unlocked ? 0xFFFFFFFF : 0xFF9F929E);
            } else {
                drawCenteredPx(context, "✦", cx, panelY + ry(iconY - 8), premium ? 0xFFFFD36A : 0xFFFF9DDE);
                drawCenteredPx(context, "HITO", cx, panelY + ry(premium ? 824 : 477), 0xFFCCB9C8);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickTrack(snapshot.free, false, mouseX, mouseY)) return true;
            if (clickTrack(snapshot.premiumTrack, true, mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickTrack(List<BattlePassSnapshot.RewardSlot> slots, boolean premium, double mouseX, double mouseY) {
        int[] centers = {278, 432, 586, 740, 894, 1048, 1202, 1356};
        for (int index = 0; index < Math.min(centers.length, slots.size()); index++) {
            int x = panelX + rx(centers[index] - 67);
            int y = panelY + ry(premium ? 630 : 286);
            if (mouseX < x || mouseX >= x + rx(134) || mouseY < y || mouseY >= y + ry(248)) continue;
            BattlePassSnapshot.RewardSlot slot = slots.get(index);
            if (slot.claimable && (!premium || snapshot.premium)) {
                BattlePassClient.request(premium ? "claim_premium" : "claim_free", slot.level);
                return true;
            }
        }
        return false;
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int centerX, int centerY, int size) {
        float scale = Math.max(1.0F, size / 16.0F);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 160.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private void drawCentered(DrawContext context, String value, int x, int y, int color) {
        drawCenteredPx(context, value, panelX + rx(x), panelY + ry(y), color);
    }

    private void drawCenteredPx(DrawContext context, String value, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }

    private int rx(int value) { return Math.round(value * panelW / (float) REF_W); }
    private int ry(int value) { return Math.round(value * panelH / (float) REF_H); }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
