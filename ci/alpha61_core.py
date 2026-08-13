from pathlib import Path
import shutil


root = Path(".")
ci = Path(__file__).resolve().parent


def replace_exact(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise AssertionError(f"missing alpha.60 marker in {path}: {old[:80]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


for relative in ("gradle.properties", "src/main/java/com/emipokemon/Emipokemon.java"):
    replace_exact(root / relative, "0.4.0-alpha.60", "0.4.0-alpha.61")

for test in (root / "src/test/java").rglob("*.java"):
    text = test.read_text(encoding="utf-8")
    text = text.replace("0.4.0-alpha.60", "0.4.0-alpha.61")
    text = text.replace("alpha60VersionIsConsistent", "alpha61VersionIsConsistent")
    text = text.replace("hasDiceResult() && elapsed < 2200L", "hasDiceResult() && elapsed < 2600L")
    test.write_text(text, encoding="utf-8")

screen = root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java"
replace_exact(screen, "import net.minecraft.client.gui.widget.TextFieldWidget;\n", """import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
""")
replace_exact(screen, "    private final long openedAt = System.currentTimeMillis();", """    private final PresentationState presentation;
    private final long openedAt;""")
replace_exact(screen, """    CasinoScreen(Screen parent, String json, String previousAmount) {
        super(Text.literal("Casino Emipokemon"));
        this.parent = parent;
        this.state = GSON.fromJson(json, CasinoNetworking.CasinoState.class);
        this.previousAmount = previousAmount;
    }

    Screen parentScreen() { return parent; }
    String amountText() { return amountField == null ? previousAmount : amountField.getText(); }
""", """    CasinoScreen(Screen parent, String json, String previousAmount, PresentationState previousPresentation) {
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
""")
replace_exact(screen, "        if (amountField != null) amountField.setText(Long.toString(amount));\n    }\n\n    private void adjustAmount", """        if (amountField != null) amountField.setText(Long.toString(amount));
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), 0.96F + multiplier * 0.015F);
    }

    private void adjustAmount""")
replace_exact(screen, "        if (amountField != null) amountField.setText(Long.toString(amount));\n    }\n\n    private boolean isRoulette", """        if (amountField != null) amountField.setText(Long.toString(amount));
        playLocal(SoundEvents.UI_BUTTON_CLICK.value(), direction < 0 ? 0.86F : 1.08F);
    }

    private boolean isRoulette""")
replace_exact(screen, "            renderRoulette(context, mouseX, mouseY);\n            return;", """            renderRoulette(context, mouseX, mouseY);
            updateCasinoSounds();
            return;""")
replace_exact(screen, "            renderFinishedGame(context, mouseX, mouseY);\n            return;", """            renderFinishedGame(context, mouseX, mouseY);
            updateCasinoSounds();
            return;""")
replace_exact(screen, "boolean rolling = hasDiceResult() && elapsed < 2200L;", "boolean rolling = \"result\".equals(state.phase()) && hasDiceResult() && elapsed < 2600L;")
replace_exact(screen, """    private boolean hasDiceResult() {
        return Pattern.compile("(\\\\d+)\\\\s*\\\\+\\\\s*(\\\\d+)").matcher(safe(state.message(), "")).find();
    }
""", """    private String diceResultSource() {
        return safe(state.tableState(), "") + " " + safe(state.message(), "");
    }

    private boolean hasDiceResult() {
        return Pattern.compile("(\\\\d+)\\\\s*\\\\+\\\\s*(\\\\d+)").matcher(diceResultSource()).find();
    }
""")
replace_exact(screen, "elapsed / 2200.0F", "elapsed / 2600.0F")
replace_exact(screen, """        float bounce = rolling ? (float)Math.abs(Math.sin(elapsed / 125.0D)) * refY(42) * energy : 0.0F;
        float travel = rolling ? (float)Math.sin(elapsed / 150.0D) * refX(20) * energy * (reverse ? -1.0F : 1.0F) : 0.0F;
        float angle = rolling ? (elapsed * (reverse ? -0.31F : 0.31F)) % 360.0F : 0.0F;
""", """        float bounce = rolling ? (float)Math.abs(Math.sin(elapsed / 105.0D)) * refY(58) * energy : 0.0F;
        float travel = rolling ? (float)Math.sin(elapsed / 135.0D) * refX(44) * energy * (reverse ? -1.0F : 1.0F) : 0.0F;
        float angle = rolling ? (elapsed * (reverse ? -0.39F : 0.39F)) % 360.0F : 0.0F;
        float pulse = rolling ? 0.88F + (float)Math.abs(Math.sin(elapsed / 115.0D)) * 0.18F : 1.0F;
""")
replace_exact(screen, "        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));\n        context.getMatrices().translate(-x", """        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        context.getMatrices().scale(pulse, pulse, 1.0F);
        context.getMatrices().translate(-x""")
replace_exact(screen, """    private void drawCasinoFrame(DrawContext context, CasinoTheme theme) {
""", """    /** Local presentation only; results, balances and rewards remain server authoritative. */
    private void updateCasinoSounds() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - openedAt);
        String game = safe(state.game(), "");
        if ("roulette".equals(game) && "result".equals(state.phase()) && rouletteResultNumber() >= 0) {
            playTimedTicks(elapsed, 3200L, 125L, 0.72F, 0.012F, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value());
            finishSoundAfter(elapsed, 3200L);
        } else if ("dice".equals(game) && "result".equals(state.phase()) && hasDiceResult()) {
            playTimedTicks(elapsed, 2600L, 170L, 0.82F, 0.018F, SoundEvents.BLOCK_STONE_HIT);
            finishSoundAfter(elapsed, 2600L);
        } else if ("slot".equals(game) && safe(state.message(), "").split("\\\\|").length >= 3) {
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
""")
replace_exact(screen, ".matcher(safe(state.message(), \"\"));\n        if (match.find()) return new int[]", ".matcher(diceResultSource());\n        if (match.find()) return new int[]")
replace_exact(screen, "        ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload", "        playActionSound(action);\n        ClientPlayNetworking.send(new CasinoNetworking.CasinoActionPayload")
replace_exact(screen, """    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    private record CasinoTheme""", """    @Override public boolean shouldPause() { return false; }
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

    private record CasinoTheme""")

client = root / "src/client/java/com/emipokemon/client/casino/CasinoClient.java"
replace_exact(client, """        String previousAmount = null;
        if (current instanceof CasinoScreen casino) {
            parent = casino.parentScreen();
            previousAmount = casino.amountText();
        }
        client.setScreen(new CasinoScreen(parent, json, previousAmount));
""", """        String previousAmount = null;
        CasinoScreen.PresentationState presentation = null;
        if (current instanceof CasinoScreen casino) {
            parent = casino.parentScreen();
            previousAmount = casino.amountText();
            presentation = casino.presentationState();
        }
        client.setScreen(new CasinoScreen(parent, json, previousAmount, presentation));
""")

release_dir = root / "release/0.4.0-alpha.61"
release_dir.mkdir(parents=True, exist_ok=True)
for name in ("CAMBIOS-0.4.0-alpha.61.md", "GUIA-INSTALACION-Y-PRUEBAS.md", "GUIA-PRUEBA-SONIDOS-Y-DADOS.md"):
    shutil.copyfile(ci / "alpha61" / name, release_dir / name)

shutil.copyfile(ci / "alpha61" / "CasinoAlpha61EffectsRegressionTest.java",
                root / "src/test/java/com/emipokemon/casino/CasinoAlpha61EffectsRegressionTest.java")
print("alpha.61 local casino sounds, persistent presentation timeline and authoritative dice animation installed")
