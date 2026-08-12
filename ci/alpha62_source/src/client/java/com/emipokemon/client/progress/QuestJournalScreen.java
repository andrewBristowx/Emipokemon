package com.emipokemon.client.progress;

import com.emipokemon.progress.JournalSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

final class QuestJournalScreen extends Screen {
    private final Screen parent;
    private final JournalSnapshot snapshot;
    private String tab;
    private String missionTrack;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    QuestJournalScreen(Screen parent, JournalSnapshot snapshot, String tab) {
        super(Text.literal("Diario de aventuras"));
        this.parent = parent;
        this.snapshot = snapshot;
        this.tab = "jobs".equals(tab) ? "jobs" : "missions";
        this.missionTrack = "adventure".equals(snapshot.questTrack) ? "adventure" : "progression";
    }

    Screen parent() {
        return parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(610, width - 24);
        panelHeight = Math.min(356, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        addDrawableChild(new JournalButtonWidget(panelX + 18, panelY + 42, 105, 20,
                Text.literal("Misiones"), () -> switchTab("missions"), () -> "missions".equals(tab)));
        addDrawableChild(new JournalButtonWidget(panelX + 128, panelY + 42, 105, 20,
                Text.literal("Trabajos"), () -> switchTab("jobs"), () -> "jobs".equals(tab)));
        addDrawableChild(new JournalButtonWidget(panelX + panelWidth - 30, panelY + 14, 18, 18,
                Text.literal("×"), this::close));
        if ("missions".equals(tab)) {
            addDrawableChild(new JournalButtonWidget(panelX + 243, panelY + 42, 105, 20,
                    Text.literal("Progresión"), () -> switchMissionTrack("progression"),
                    () -> "progression".equals(missionTrack)));
            addDrawableChild(new JournalButtonWidget(panelX + 353, panelY + 42, 105, 20,
                    Text.literal("Aventura"), () -> switchMissionTrack("adventure"),
                    () -> "adventure".equals(missionTrack)));
            initMissionButtons();
        }
        else initJobButtons();
    }

    private void initMissionButtons() {
        JournalSnapshot.QuestView quest = snapshot.quest;
        if (quest != null && quest.complete && !quest.claimed) {
            addDrawableChild(new JournalButtonWidget(panelX + panelWidth - 190, panelY + panelHeight - 42, 170, 22,
                    Text.literal("Reclamar recompensa"), () -> {
                        ProgressionClient.sendAction("claim", missionTrack);
                    }));
        }
    }

    private void initJobButtons() {
        int rowY = panelY + 82;
        int rowHeight = Math.max(27, Math.min(34, (panelHeight - 114) / Math.max(1, snapshot.jobs.size())));
        for (JournalSnapshot.JobView job : snapshot.jobs) {
            JournalButtonWidget button = new JournalButtonWidget(panelX + panelWidth - 82, rowY + 3, 60, 20,
                    Text.literal(job.active ? "Quitar" : "Elegir"), () -> {
                        ProgressionClient.sendAction(job.active ? "job_leave" : "job_join", job.id);
                    }, () -> job.active);
            addDrawableChild(button);
            rowY += rowHeight;
        }
    }

    private void switchTab(String next) {
        if (tab.equals(next)) return;
        tab = next;
        clearAndInit();
    }

    private void switchMissionTrack(String next) {
        if (missionTrack.equals(next)) return;
        ProgressionClient.requestOpen("missions:" + next);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw our own background before the journal. Screen#render invokes
        // renderBackground again, so that method is overridden below as a no-op;
        // otherwise Minecraft's blur pass runs over everything drawn here.
        context.fill(0, 0, width, height, 0xA30A0710);
        drawPanel(context);
        if ("missions".equals(tab)) drawMissions(context);
        else drawJobs(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally empty. The journal supplies its own dimmed background in
        // render(), and letting Screen render the vanilla background here would
        // apply the post-process blur after the panel and text have been drawn.
    }

    private void drawPanel(DrawContext context) {
        context.fill(panelX + 6, panelY + 8, panelX + panelWidth + 6, panelY + panelHeight + 8, 0x82000000);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF5140C19);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 4, 0xFFFF9DDE);
        context.fill(panelX, panelY + 4, panelX + panelWidth, panelY + 7, 0xFF75418E);
        context.fill(panelX, panelY + panelHeight - 4, panelX + panelWidth, panelY + panelHeight, 0xFF75418E);
        context.fill(panelX, panelY, panelX + 3, panelY + panelHeight, 0xFF9D5AAF);
        context.fill(panelX + panelWidth - 3, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF9D5AAF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Diario de aventuras de Emi"), panelX + panelWidth / 2, panelY + 20, 0xFFFFD8F1);
        context.fill(panelX + 14, panelY + 70, panelX + panelWidth - 14, panelY + 71, 0x706A3C75);
    }

    private void drawMissions(DrawContext context) {
        int leftWidth = Math.min(176, panelWidth / 3);
        int leftX = panelX + 16;
        int top = panelY + 82;
        context.fill(leftX, top, leftX + leftWidth, panelY + panelHeight - 18, 0x8E28152F);
        context.drawTextWithShadow(textRenderer, Text.literal("CAPÍTULOS"), leftX + 10, top + 9, 0xFFEAB7DD);
        int chapterY = top + 27;
        for (JournalSnapshot.ChapterView chapter : snapshot.chapters) {
            boolean current = snapshot.quest != null && chapter.id.equals(snapshot.quest.chapter);
            int color = current ? 0xFF613164 : chapter.unlocked ? 0xD73C2346 : 0xA6252028;
            context.fill(leftX + 7, chapterY, leftX + leftWidth - 7, chapterY + 34, color);
            String marker = chapter.unlocked ? (current ? "◆ " : "◇ ") : "[X] ";
            context.drawTextWithShadow(textRenderer, Text.literal(marker + chapter.title), leftX + 13, chapterY + 6,
                    current ? 0xFFFFD4ED : 0xFFD2B4CE);
            context.drawTextWithShadow(textRenderer, Text.literal(chapter.complete + "/" + chapter.total), leftX + 13, chapterY + 19,
                    chapter.complete >= chapter.total ? 0xFF91E6B1 : 0xFFAD91AF);
            chapterY += 39;
        }

        int bodyX = leftX + leftWidth + 16;
        int bodyRight = panelX + panelWidth - 18;
        JournalSnapshot.QuestView quest = snapshot.quest;
        if (quest == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("¡Has completado todas las misiones disponibles!"),
                    (bodyX + bodyRight) / 2, top + 80, 0xFFFFD36A);
            return;
        }
        context.drawTextWithShadow(textRenderer, Text.literal("Capítulo " + quest.chapter + " — " + quest.chapterTitle), bodyX, top + 4, 0xFFBD8EC8);
        context.drawTextWithShadow(textRenderer, Text.literal(quest.title), bodyX, top + 24, 0xFFFFCDE9);
        drawWrapped(context, quest.description, bodyX, top + 43, bodyRight - bodyX, 0xFFE2D4E3);

        int objectiveY = top + 101;
        context.fill(bodyX, objectiveY, bodyRight, objectiveY + 64, 0xA52E1A38);
        context.drawTextWithShadow(textRenderer, Text.literal("OBJETIVO"), bodyX + 10, objectiveY + 8, 0xFFF5A8D5);
        context.drawTextWithShadow(textRenderer, Text.literal(quest.objective), bodyX + 10, objectiveY + 24, 0xFFFFFFFF);
        String progress = quest.progress + " / " + quest.target;
        context.drawTextWithShadow(textRenderer, Text.literal(progress), bodyRight - textRenderer.getWidth(progress) - 10, objectiveY + 24,
                quest.complete ? 0xFF9CF0B6 : 0xFFFFE6F6);
        int barLeft = bodyX + 10;
        int barRight = bodyRight - 10;
        int filled = (int) ((barRight - barLeft) * MathHelper.clamp((double) quest.progress / Math.max(1L, quest.target), 0.0D, 1.0D));
        context.fill(barLeft, objectiveY + 43, barRight, objectiveY + 52, 0xFF170D1B);
        context.fill(barLeft + 1, objectiveY + 44, barLeft + Math.max(1, filled), objectiveY + 51,
                quest.complete ? 0xFF73D99A : 0xFFE37ABF);

        int rewardY = objectiveY + 76;
        context.drawTextWithShadow(textRenderer, Text.literal("RECOMPENSAS"), bodyX, rewardY, 0xFFF5A8D5);
        context.drawTextWithShadow(textRenderer, Text.literal(quest.coins + " Michicoins"), bodyX, rewardY + 18, 0xFFFFD36A);
        int itemY = rewardY + 34;
        for (String item : quest.items) {
            context.drawTextWithShadow(textRenderer, Text.literal("• " + item), bodyX, itemY, 0xFFDCC5DE);
            itemY += 13;
        }
        String trackName = "adventure".equals(missionTrack) ? "Aventura" : "Progresión";
        context.drawTextWithShadow(textRenderer, Text.literal(trackName + ": " + snapshot.completedQuests + "/" + snapshot.totalQuests),
                leftX + 10, panelY + panelHeight - 33, 0xFFAD91AF);
    }

    private void drawJobs(DrawContext context) {
        int rowY = panelY + 82;
        int rowHeight = Math.max(27, Math.min(34, (panelHeight - 114) / Math.max(1, snapshot.jobs.size())));
        for (JournalSnapshot.JobView job : snapshot.jobs) {
            int color = job.active ? 0xD04B2B58 : 0xA52B1934;
            context.fill(panelX + 18, rowY, panelX + panelWidth - 18, rowY + rowHeight - 3, color);
            if (job.active) context.fill(panelX + 18, rowY, panelX + 22, rowY + rowHeight - 3, 0xFFFF9DDE);
            context.drawTextWithShadow(textRenderer, Text.literal((job.active ? "◆ " : "◇ ") + job.name + "  Nv. " + job.level),
                    panelX + 30, rowY + 5, job.active ? 0xFFFFD6ED : 0xFFE8CAE1);
            context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(job.description, panelWidth - 300)),
                    panelX + 30, rowY + 17, 0xFFB99DB7);
            int barLeft = panelX + panelWidth - 208;
            int barRight = panelX + panelWidth - 92;
            double fraction = job.nextLevel <= job.levelStart ? 1.0D
                    : (double) (job.xp - job.levelStart) / (double) (job.nextLevel - job.levelStart);
            int filled = (int) ((barRight - barLeft) * MathHelper.clamp(fraction, 0.0D, 1.0D));
            context.fill(barLeft, rowY + 9, barRight, rowY + 16, 0xFF160D1A);
            context.fill(barLeft + 1, rowY + 10, barLeft + Math.max(1, filled), rowY + 15, 0xFFE27BBD);
            rowY += rowHeight;
        }
        String slots = snapshot.maxActiveJobs >= snapshot.jobs.size()
                ? "Trabajos activos: " + snapshot.activeJobCount + " / todos"
                : "Trabajos activos: " + snapshot.activeJobCount + " / " + snapshot.maxActiveJobs;
        context.drawTextWithShadow(textRenderer, Text.literal(slots),
                panelX + 20, panelY + panelHeight - 26, 0xFFCCB4CC);
    }

    private void drawWrapped(DrawContext context, String value, int x, int y, int width, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(value), width);
        for (int index = 0; index < Math.min(3, lines.size()); index++) {
            context.drawTextWithShadow(textRenderer, lines.get(index), x, y + index * 11, color);
        }
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
