package com.emipokemon.client.rewards;

import com.emipokemon.Emipokemon;
import com.emipokemon.client.render.PokemonPortraitRenderer;
import com.emipokemon.registry.ModRegistries;
import com.emipokemon.rewards.DailyRewardSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class DailyRewardScreen extends Screen {
    private static final Identifier BACKGROUND = Identifier.of(Emipokemon.MOD_ID, "textures/gui/daily_reward.png");
    private static final int REF_W = 1536;
    private static final int REF_H = 1024;
    private final Screen parent;
    private final DailyRewardSnapshot snapshot;
    private final long openedAt = System.currentTimeMillis();
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    DailyRewardScreen(Screen parent, DailyRewardSnapshot snapshot) {
        super(Text.literal("Recompensa diaria"));
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
        addDrawableChild(new PassButtonWidget(panelX + rx(1432), panelY + ry(31), rx(74), ry(64),
                Text.literal("×"), button -> close()));
        PassButtonWidget claim = new PassButtonWidget(panelX + rx(570), panelY + ry(674), rx(330), ry(62),
                Text.literal(snapshot.eligible ? "ABRIR RECOMPENSA" : "RECLAMADO"), button -> DailyRewardClient.send("claim"));
        claim.active = snapshot.eligible;
        addDrawableChild(claim);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xD00A0710);
        context.drawTexture(BACKGROUND, panelX, panelY, panelW, panelH, 0, 0, REF_W, REF_H, REF_W, REF_H);
        drawHeader(context);
        drawReveal(context);
        drawPossibleRewards(context);
        drawStatus(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHeader(DrawContext context) {
        drawCentered(context, "RECOMPENSA DIARIA", 748, 108, 0xFFFFE4F4);
        drawCentered(context, snapshot.eligible ? "Tu cápsula está lista" : "Vuelve mañana", 748, 146,
                snapshot.eligible ? 0xFFFFD36A : 0xFFCEBFD0);
    }

    private void drawReveal(DrawContext context) {
        int cx = panelX + rx(748);
        int cy = panelY + ry(490);
        DailyRewardSnapshot.RewardView reward = snapshot.revealed;
        if (reward == null) {
            drawCenteredPx(context, snapshot.eligible ? "?" : "✓", cx, cy - ry(35),
                    snapshot.eligible ? 0xFFFFD36A : 0xFF9CE3B0);
            drawCenteredPx(context, snapshot.eligible ? "PREMIO SORPRESA" : snapshot.lastReward,
                    cx, panelY + ry(598), 0xFFFFFFFF);
            return;
        }
        long elapsed = System.currentTimeMillis() - openedAt;
        if (elapsed < 1_250L && snapshot.possibleRewards != null && !snapshot.possibleRewards.isEmpty()) {
            int cycling = (int) ((elapsed / 95L) % snapshot.possibleRewards.size());
            drawRewardIcon(context, snapshot.possibleRewards.get(cycling), cx, cy, rx(80));
            drawCenteredPx(context, "ABRIENDO CÁPSULA…", cx, panelY + ry(610), 0xFFFFF1FA);
            return;
        }
        float pulse = elapsed < 1400L ? 0.78F + (float) Math.abs(Math.sin(elapsed / 95.0D)) * 0.28F : 1.0F;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0.0F);
        context.getMatrices().scale(pulse, pulse, 1.0F);
        context.getMatrices().translate(-cx, -cy, 0.0F);
        drawRewardIcon(context, reward, cx, cy, rx(92));
        context.getMatrices().pop();
        drawCenteredPx(context, reward.label, cx, panelY + ry(610), 0xFFFFF1FA);
        if (reward.shiny) drawCenteredPx(context, "✦ SHINY ✦", cx, panelY + ry(641), 0xFFFFD36A);
    }

    private void drawPossibleRewards(DrawContext context) {
        int[] centers = {105, 283, 461, 639, 817, 995, 1173};
        List<DailyRewardSnapshot.RewardView> rewards = snapshot.possibleRewards == null ? List.of() : snapshot.possibleRewards;
        int[] preferred = rewards.size() >= 8 ? new int[] {0, 1, 2, 4, 5, 6, 7} : new int[] {0, 1, 2, 3, 4, 5, 6};
        for (int index = 0; index < centers.length && index < rewards.size(); index++) {
            DailyRewardSnapshot.RewardView reward = rewards.get(Math.min(preferred[index], rewards.size() - 1));
            int cx = panelX + rx(centers[index]);
            int cy = panelY + ry(862);
            drawRewardIcon(context, reward, cx, cy, rx(42));
            String label = textRenderer.trimToWidth(reward.label, rx(125));
            drawCenteredPx(context, label, cx, panelY + ry(942), 0xFFE8D8E7);
        }
    }

    private void drawStatus(DrawContext context) {
        int cx = panelX + rx(1371);
        drawCenteredPx(context, "RACHA", cx, panelY + ry(190), 0xFFFFD36A);
        drawCenteredPx(context, snapshot.streak + " día" + (snapshot.streak == 1 ? "" : "s"), cx, panelY + ry(225), 0xFFFFFFFF);
        drawCenteredPx(context, "RECLAMOS", cx, panelY + ry(565), 0xFFFFD36A);
        drawCenteredPx(context, Integer.toString(snapshot.totalClaims), cx, panelY + ry(600), 0xFFFFFFFF);
        drawCenteredPx(context, "PRÓXIMA", cx, panelY + ry(705), 0xFFFFD36A);
        drawCenteredPx(context, remaining(), cx, panelY + ry(742), 0xFFFFFFFF);
        if (snapshot.message != null && !snapshot.message.isBlank()) {
            List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(Text.literal(snapshot.message), rx(210));
            int y = panelY + ry(805);
            for (int index = 0; index < Math.min(4, lines.size()); index++) {
                net.minecraft.text.OrderedText line = lines.get(index);
                context.drawTextWithShadow(textRenderer, line, cx - textRenderer.getWidth(line) / 2,
                        y + index * 11, 0xFFFFDDEC);
            }
        }
    }

    private String remaining() {
        long millis = Math.max(0L, snapshot.nextClaimEpochMillis - System.currentTimeMillis());
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return String.format(java.util.Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    private void drawRewardIcon(DrawContext context, DailyRewardSnapshot.RewardView reward, int cx, int cy, int size) {
        if (reward == null) return;
        if ("POKEMON".equals(reward.type)) {
            if (reward.speciesId != null && !reward.speciesId.isBlank()
                    && PokemonPortraitRenderer.draw(context, reward.speciesId, cx, cy + size / 2, size)) return;
            drawCenteredPx(context, "◉", cx, cy - 7, 0xFFFFD36A);
            return;
        }
        ItemStack stack = rewardItem(reward);
        if (!stack.isEmpty()) {
            drawLargeItem(context, stack, cx, cy, size);
            if (reward.amount > 1) drawCenteredPx(context, "×" + reward.amount, cx + size / 2, cy + size / 3, 0xFFFFFFFF);
            return;
        }
        if ("MICHICOINS".equals(reward.type)) {
            drawCenteredPx(context, "M", cx, cy - 7, 0xFFFFD36A);
            return;
        }
        drawCenteredPx(context, "✦", cx, cy - 7, 0xFFFF9DDE);
    }

    private ItemStack rewardItem(DailyRewardSnapshot.RewardView reward) {
        if ("EMI_ROLLS".equals(reward.type)) return new ItemStack(ModRegistries.EMI_SPECIAL_BANNER_TICKET);
        if ("STANDARD_ROLLS".equals(reward.type)) return new ItemStack(ModRegistries.GACHA_TICKET);
        if (!"ITEM".equals(reward.type)) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(reward.value);
        if (id == null || !Registries.ITEM.containsId(id)) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int centerX, int centerY, int size) {
        float scale = Math.max(1.0F, size / 16.0F);
        context.getMatrices().push();
        context.getMatrices().translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 170.0F);
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
