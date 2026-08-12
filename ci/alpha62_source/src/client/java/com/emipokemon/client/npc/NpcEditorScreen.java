package com.emipokemon.client.npc;

import com.emipokemon.npc.NpcNetworking.NpcEditorState;
import com.emipokemon.npc.NpcNetworking.SaveMediaPayload;
import com.emipokemon.npc.NpcNetworking.SaveNpcPayload;
import com.emipokemon.npc.NpcNetworking.UploadChunkPayload;
import com.emipokemon.npc.NpcNetworking.UrlAssetPayload;
import com.emipokemon.visual.VisualAssetService;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class NpcEditorScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final int CHUNK_BYTES = 18_000;
    private final Screen parent;
    private final NpcEditorState state;
    private final List<TextFieldWidget> teamFields = new ArrayList<>();
    private TextFieldWidget nameField;
    private TextFieldWidget dialogueField;
    private TextFieldWidget rewardsField;
    private TextFieldWidget urlField;
    private TextFieldWidget widthField;
    private TextFieldWidget heightField;
    private AdminButtonWidget repeatableButton;
    private boolean rewardRepeatable;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    NpcEditorScreen(Screen parent, String json) {
        super(Text.literal("Editor Emipokemon"));
        this.parent = parent;
        this.state = GSON.fromJson(json, NpcEditorState.class);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(700, width - 20);
        panelHeight = Math.min("npc".equals(state.kind()) ? 520 : 285, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        if ("npc".equals(state.kind())) initNpc(); else initMedia();
    }

    private void initNpc() {
        int left = panelX + 24;
        int contentWidth = panelWidth - 48;
        nameField = field(left, panelY + 63, contentWidth, 20, state.name(), 48);
        dialogueField = field(left, panelY + 108, contentWidth, 20, state.dialogue(), 2048);
        teamFields.clear();
        int columnWidth = (contentWidth - 12) / 2;
        for (int index = 0; index < 6; index++) {
            int column = index % 2;
            int row = index / 2;
            String value = index < state.team().size() ? state.team().get(index) : "";
            teamFields.add(field(left + column * (columnWidth + 12), panelY + 160 + row * 35,
                    columnWidth, 20, value, 256));
        }
        rewardsField = field(left, panelY + 282, contentWidth - 170, 20, String.join(", ", state.rewards()), 1024);
        rewardRepeatable = state.rewardRepeatable();
        repeatableButton = addDrawableChild(new AdminButtonWidget(left + contentWidth - 158, panelY + 282,
                158, 20, repeatableLabel(), this::toggleRepeatable));
        urlField = field(left, panelY + 337, contentWidth, 20, "", 2048);
        addDrawableChild(new AdminButtonWidget(left, panelY + 372, 135, 24, Text.literal("Aplicar URL HTTPS"), this::applyUrl));
        addDrawableChild(new AdminButtonWidget(left + 145, panelY + 372, 135, 24, Text.literal("Subir skin PNG"), this::chooseFile));
        addDrawableChild(new AdminButtonWidget(panelX + panelWidth - 250, panelY + panelHeight - 38,
                110, 24, Text.literal("Guardar"), this::save));
        addDrawableChild(new AdminButtonWidget(panelX + panelWidth - 130, panelY + panelHeight - 38,
                105, 24, Text.literal("Cerrar"), this::close));
    }

    private void initMedia() {
        int left = panelX + 24;
        int contentWidth = panelWidth - 48;
        widthField = field(left, panelY + 68, 120, 20, Float.toString(state.width()), 8);
        heightField = field(left + 140, panelY + 68, 120, 20, Float.toString(state.height()), 8);
        urlField = field(left, panelY + 119, contentWidth, 20, "", 2048);
        addDrawableChild(new AdminButtonWidget(left, panelY + 154, 150, 24, Text.literal("Aplicar URL HTTPS"), this::applyUrl));
        addDrawableChild(new AdminButtonWidget(left + 160, panelY + 154, 165, 24, Text.literal("Subir PNG o GIF"), this::chooseFile));
        addDrawableChild(new AdminButtonWidget(panelX + panelWidth - 250, panelY + panelHeight - 38,
                110, 24, Text.literal("Guardar"), this::save));
        addDrawableChild(new AdminButtonWidget(panelX + panelWidth - 130, panelY + panelHeight - 38,
                105, 24, Text.literal("Cerrar"), this::close));
    }

    private TextFieldWidget field(int x, int y, int width, int height, String text, int max) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, height, Text.empty());
        field.setMaxLength(max);
        field.setText(text == null ? "" : text);
        addDrawableChild(field);
        return field;
    }

    private void save() {
        if ("npc".equals(state.kind())) {
            List<String> team = teamFields.stream().map(TextFieldWidget::getText).toList();
            List<String> rewards = java.util.Arrays.stream(rewardsField.getText().split(","))
                    .map(String::strip).filter(value -> !value.isBlank()).toList();
            NpcEditorState updated = new NpcEditorState("npc", state.id(), nameField.getText(),
                    dialogueField.getText(), team, rewards, rewardRepeatable, 0.0f, 0.0f);
            ClientPlayNetworking.send(new SaveNpcPayload(GSON.toJson(updated)));
        } else {
            try {
                float width = Float.parseFloat(widthField.getText());
                float height = Float.parseFloat(heightField.getText());
                ClientPlayNetworking.send(new SaveMediaPayload(GSON.toJson(new NpcEditorState(
                        "media", state.id(), "", "", List.of(), List.of(), false, width, height))));
            } catch (NumberFormatException exception) {
                message("§cAncho y alto deben ser números.");
            }
        }
    }

    private void toggleRepeatable() {
        rewardRepeatable = !rewardRepeatable;
        repeatableButton.setMessage(repeatableLabel());
    }

    private Text repeatableLabel() {
        return Text.literal(rewardRepeatable ? "Repetible: SÍ" : "Repetible: NO");
    }

    private void applyUrl() {
        String url = urlField.getText().strip();
        if (url.isBlank()) {
            message("§eEscribe una URL HTTPS directa.");
            return;
        }
        ClientPlayNetworking.send(new UrlAssetPayload(state.kind(), state.id(), url));
    }

    private void chooseFile() {
        CompletableFuture.supplyAsync(() -> TinyFileDialogs.tinyfd_openFileDialog(
                "Seleccionar " + ("npc".equals(state.kind()) ? "skin PNG" : "imagen PNG o GIF"),
                "", null, "PNG/GIF", false)).thenAccept(selected -> {
            if (selected == null || selected.isBlank() || client == null) return;
            try {
                Path path = Path.of(selected);
                long size = Files.size(path);
                if (size < 1 || size > VisualAssetService.MAX_BYTES) {
                    client.execute(() -> message("§cEl archivo debe ocupar entre 1 byte y 4 MiB."));
                    return;
                }
                byte[] bytes = Files.readAllBytes(path);
                client.execute(() -> upload(bytes));
            } catch (Exception exception) {
                client.execute(() -> message("§cNo se pudo leer el archivo: " + exception.getMessage()));
            }
        });
    }

    private void upload(byte[] bytes) {
        int total = Math.max(1, (bytes.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
        for (int index = 0; index < total; index++) {
            int start = index * CHUNK_BYTES;
            int length = Math.min(CHUNK_BYTES, bytes.length - start);
            byte[] part = java.util.Arrays.copyOfRange(bytes, start, start + length);
            ClientPlayNetworking.send(new UploadChunkPayload(state.kind(), state.id(), index, total,
                    Base64.getEncoder().encodeToString(part)));
        }
        message("§7Archivo enviado para validación del servidor...");
    }

    private void message(String value) {
        if (client != null && client.player != null) client.player.sendMessage(Text.literal(value), false);
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
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(("npc".equals(state.kind()) ? "Editor de NPC — " : "Editor multimedia — ") + state.id()),
                panelX + panelWidth / 2, panelY + 19, 0xFFFFD8F1);
        int left = panelX + 24;
        if ("npc".equals(state.kind())) {
            label(context, "Nombre visible", left, panelY + 49);
            label(context, "Diálogo", left, panelY + 94);
            label(context, "Equipo Pokémon — formato Cobblemon, por ejemplo: pikachu level=50", left, panelY + 144);
            label(context, "Recompensas — mod:item*cantidad, separadas por coma", left, panelY + 268);
            label(context, "Skin por URL HTTPS directa", left, panelY + 323);
        } else {
            label(context, "Ancho", left, panelY + 53);
            label(context, "Alto", left + 140, panelY + 53);
            label(context, "Imagen o GIF por URL HTTPS directa", left, panelY + 104);
        }
        context.drawTextWithShadow(textRenderer, Text.literal("Los archivos se validan y guardan en el servidor."),
                left, panelY + panelHeight - 34, 0xFFA98CA7);
    }

    private void label(DrawContext context, String value, int x, int y) {
        context.drawTextWithShadow(textRenderer, Text.literal(value), x, y, 0xFFF0B9DE);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
