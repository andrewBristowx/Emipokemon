package com.emipokemon.client.admin;

import com.emipokemon.admin.AdminNetworking.AdminSnapshot;
import com.emipokemon.admin.AdminNetworking.HologramState;
import com.emipokemon.admin.AdminNetworking.ProductState;
import com.emipokemon.client.npc.AdminButtonWidget;
import com.emipokemon.config.EmipokemonConfig;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.banner.BannerDefinition;
import com.google.gson.Gson;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

final class AdminScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final String[] TABS = {"Equilibrio", "Banners", "Tienda", "Hologramas", "Auditoría"};
    private final Screen parent;
    private final AdminSnapshot state;
    private final String serverMessage;
    private final int initialTab;
    private final List<LabeledField> fields = new ArrayList<>();
    private int tab;
    private int panelX, panelY, panelW, panelH;
    private int bannerIndex;
    private int productIndex;
    private int hologramIndex;

    AdminScreen(Screen parent, String json, String message, int tab) {
        super(Text.literal("Administración Emipokemon"));
        this.parent = parent instanceof AdminScreen current ? current.parent : parent;
        AdminSnapshot parsed = GSON.fromJson(json, AdminSnapshot.class);
        this.state = parsed == null ? new AdminSnapshot() : parsed;
        this.serverMessage = message == null ? "" : message;
        this.initialTab = Math.clamp(tab, 0, TABS.length - 1);
    }

    int tab() { return tab; }

    @Override
    protected void init() {
        tab = initialTab;
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        fields.clear();
        panelW = Math.min(820, width - 20);
        panelH = Math.min(500, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int tabW = Math.max(92, (panelW - 36) / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            int selected = i;
            addDrawableChild(new AdminButtonWidget(panelX + 18 + i * tabW, panelY + 42, tabW - 6, 22,
                    Text.literal((tab == i ? "◆ " : "") + TABS[i]), () -> { tab = selected; rebuild(); }));
        }
        switch (tab) {
            case 0 -> initBalance();
            case 1 -> initBanners();
            case 2 -> initShop();
            case 3 -> initHolograms();
            default -> initAudit();
        }
        addDrawableChild(new AdminButtonWidget(panelX + panelW - 228, panelY + panelH - 34, 96, 22,
                Text.literal("Actualizar"), () -> AdminClient.send("refresh", "")));
        addDrawableChild(new AdminButtonWidget(panelX + panelW - 122, panelY + panelH - 34, 104, 22,
                Text.literal("Cerrar"), this::close));
    }

    private void initBalance() {
        EmipokemonConfig.BalanceSettings b = state.balance == null ? new EmipokemonConfig.BalanceSettings() : state.balance;
        int x = panelX + 32, y = panelY + 95, w = 180;
        field("Segundos de actividad", x, y, w, Integer.toString(b.activeRewardSeconds), 8);
        field("Monedas por actividad", x + 250, y, w, Long.toString(b.activeRewardCoins), 12);
        field("Monedas directas ×", x, y + 54, w, number(b.directCoinMultiplier), 10);
        field("Monedas de trabajos ×", x + 250, y + 54, w, number(b.jobCoinMultiplier), 10);
        field("XP de trabajos ×", x, y + 108, w, number(b.jobXpMultiplier), 10);
        field("Monedas de misiones ×", x + 250, y + 108, w, number(b.questCoinMultiplier), 10);
        field("Precios Poké Mart ×", x, y + 162, w, number(b.shopPriceMultiplier), 10);
        addDrawableChild(new AdminButtonWidget(panelX + 32, panelY + panelH - 72, 180, 24,
                Text.literal("Guardar equilibrio"), this::saveBalance));
    }

    private void saveBalance() {
        try {
            EmipokemonConfig.BalanceSettings b = new EmipokemonConfig.BalanceSettings();
            b.activeRewardSeconds = integer(0); b.activeRewardCoins = longValue(1);
            b.directCoinMultiplier = decimal(2); b.jobCoinMultiplier = decimal(3);
            b.jobXpMultiplier = decimal(4); b.questCoinMultiplier = decimal(5); b.shopPriceMultiplier = decimal(6);
            AdminClient.send("save_balance", GSON.toJson(b));
        } catch (Exception e) { clientMessage("§cRevisa los valores numéricos."); }
    }

    private void initBanners() {
        if (state.banners == null) state.banners = new ArrayList<>();
        bannerIndex = state.banners.isEmpty() ? 0 : Math.floorMod(bannerIndex, state.banners.size());
        int y = panelY + 82;
        addDrawableChild(new AdminButtonWidget(panelX + 24, y, 38, 22, Text.literal("<"), () -> cycleBanner(-1)));
        addDrawableChild(new AdminButtonWidget(panelX + 68, y, 38, 22, Text.literal(">"), () -> cycleBanner(1)));
        if (state.banners.isEmpty()) {
            BannerDefinition created = new BannerDefinition(); created.id = "nuevo_banner"; state.banners.add(created);
        }
        BannerDefinition b = state.banners.get(bannerIndex);
        int left = panelX + 24;
        field("ID", left, y + 48, 180, b.id, 32);
        field("Nombre", left + 205, y + 48, 270, b.displayName, 80);
        field("Habilitado (true/false)", left + 500, y + 48, 150, Boolean.toString(b.enabled), 5);
        field("Inicio UTC epoch ms (0=sin límite)", left, y + 104, 215, Long.toString(b.startsAtEpochMillis), 20);
        field("Fin UTC epoch ms (0=sin límite)", left + 240, y + 104, 215, Long.toString(b.endsAtEpochMillis), 20);
        field("Prioridad", left + 480, y + 104, 100, Integer.toString(b.rotationPriority), 8);
        field("Generaciones CSV (vacío=todas)", left, y + 160, 260, joinInts(b.generations), 50);
        field("Destacados: pokemon=multiplicador, ...", left + 285, y + 160, 365, joinMap(b.featuredSpecies), 512);
        field("Pesos: COMMON=45,UNCOMMON=27,RARE=15,EPIC=8,LEGENDARY=4,MYTHICAL=1",
                left, y + 216, 650, joinMap(b.tierWeights), 512);
        addDrawableChild(new AdminButtonWidget(left, panelY + panelH - 72, 155, 24,
                Text.literal("Guardar banner"), () -> saveBanner(b)));
        addDrawableChild(new AdminButtonWidget(left + 165, panelY + panelH - 72, 190, 24,
                Text.literal("Asignar a máquina mirada"), () -> AdminClient.send("machine_banner", "{\"bannerId\":\"" + escape(fields.get(0).field.getText()) + "\"}")));
        addDrawableChild(new AdminButtonWidget(left + 365, panelY + panelH - 72, 145, 24,
                Text.literal("Reiniciar máquina"), () -> AdminClient.send("machine_reset", "")));
    }

    private void cycleBanner(int amount) {
        if (!state.banners.isEmpty()) bannerIndex = Math.floorMod(bannerIndex + amount, state.banners.size());
        rebuild();
    }

    private void saveBanner(BannerDefinition b) {
        try {
            b.id = value(0).strip().toLowerCase(Locale.ROOT); b.displayName = value(1); b.enabled = Boolean.parseBoolean(value(2));
            b.startsAtEpochMillis = Long.parseLong(value(3)); b.endsAtEpochMillis = Long.parseLong(value(4));
            b.rotationPriority = Integer.parseInt(value(5)); b.generations = parseInts(value(6));
            b.featuredSpecies = parseMap(value(7)); b.tierWeights = parseMap(value(8));
            AdminClient.send("save_banner", GSON.toJson(b));
        } catch (Exception e) { clientMessage("§cFormato inválido en el banner: " + e.getMessage()); }
    }

    private void initShop() {
        if (state.products == null || state.products.isEmpty()) return;
        productIndex = Math.floorMod(productIndex, state.products.size());
        ProductState p = state.products.get(productIndex);
        int y = panelY + 92;
        addDrawableChild(new AdminButtonWidget(panelX + 28, y, 38, 22, Text.literal("<"), () -> cycleProduct(-1)));
        addDrawableChild(new AdminButtonWidget(panelX + 72, y, 38, 22, Text.literal(">"), () -> cycleProduct(1)));
        field("ID protegido por servidor", panelX + 28, y + 55, 250, p.id(), 64).setEditable(false);
        field("Precio base (1–10 000 000)", panelX + 303, y + 55, 220, Long.toString(p.price()), 12);
        addDrawableChild(new AdminButtonWidget(panelX + 28, y + 120, 170, 24, Text.literal("Guardar precio"), () -> {
            try { AdminClient.send("save_price", "{\"productId\":\"" + escape(value(0)) + "\",\"price\":" + Long.parseLong(value(1)) + "}"); }
            catch (Exception e) { clientMessage("§cPrecio inválido."); }
        }));
    }

    private void cycleProduct(int amount) { productIndex = Math.floorMod(productIndex + amount, state.products.size()); rebuild(); }

    private void initHolograms() {
        if (state.holograms == null) state.holograms = new ArrayList<>();
        HologramState h = state.holograms.isEmpty() ? null : state.holograms.get(Math.floorMod(hologramIndex, state.holograms.size()));
        int y = panelY + 88;
        addDrawableChild(new AdminButtonWidget(panelX + 28, y, 38, 22, Text.literal("<"), () -> cycleHologram(-1)));
        addDrawableChild(new AdminButtonWidget(panelX + 72, y, 38, 22, Text.literal(">"), () -> cycleHologram(1)));
        field("ID", panelX + 28, y + 54, 180, h == null ? "nuevo_holograma" : h.id(), 32);
        field("Texto", panelX + 228, y + 54, 430, h == null ? "Bienvenido" : h.text(), 512);
        field("Escala (0.25–8)", panelX + 28, y + 112, 180, h == null ? "1.0" : Float.toString(h.scale()), 8);
        field("Color RRGGBB", panelX + 228, y + 112, 180, h == null ? "FFFFFF" : h.color(), 6);
        addDrawableChild(new AdminButtonWidget(panelX + 28, y + 176, 120, 24, Text.literal("Crear aquí"), () -> hologramAction("hologram_create")));
        addDrawableChild(new AdminButtonWidget(panelX + 158, y + 176, 120, 24, Text.literal("Actualizar"), () -> hologramAction("hologram_update")));
        addDrawableChild(new AdminButtonWidget(panelX + 288, y + 176, 120, 24, Text.literal("Mover aquí"), () -> hologramAction("hologram_move")));
        addDrawableChild(new AdminButtonWidget(panelX + 418, y + 176, 120, 24, Text.literal("Eliminar"), () -> hologramAction("hologram_delete")));
    }

    private void cycleHologram(int amount) { if (!state.holograms.isEmpty()) hologramIndex = Math.floorMod(hologramIndex + amount, state.holograms.size()); rebuild(); }
    private void hologramAction(String action) {
        try {
            float scale = Float.parseFloat(value(2));
            AdminClient.send(action, "{\"id\":\"" + escape(value(0).toLowerCase(Locale.ROOT)) + "\",\"text\":\""
                    + escape(value(1)) + "\",\"scale\":" + scale + ",\"color\":\"" + escape(value(3)) + "\"}");
        } catch (Exception e) { clientMessage("§cEscala o datos inválidos."); }
    }

    private void initAudit() { }

    private TextFieldWidget field(String label, int x, int y, int w, String text, int max) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal(label));
        f.setMaxLength(max); f.setText(text == null ? "" : text); addDrawableChild(f); fields.add(new LabeledField(label, f)); return f;
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0x90000000);
        c.fill(panelX + 7, panelY + 8, panelX + panelW + 7, panelY + panelH + 8, 0x75000000);
        c.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF5100915);
        c.fill(panelX, panelY, panelX + panelW, panelY + 5, 0xFFFF9DDE);
        c.fill(panelX, panelY + 5, panelX + panelW, panelY + 8, 0xFF75418E);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("Panel administrativo Emipokemon"), panelX + panelW / 2, panelY + 18, 0xFFFFD8F1);
        for (LabeledField item : fields) c.drawTextWithShadow(textRenderer, Text.literal(item.label), item.field.getX(), item.field.getY() - 13, 0xFFF0B9DE);
        if (tab == 1 && state.banners != null && !state.banners.isEmpty()) {
            BannerDefinition b = state.banners.get(Math.floorMod(bannerIndex, state.banners.size()));
            c.drawTextWithShadow(textRenderer, Text.literal("Banner " + (bannerIndex + 1) + "/" + state.banners.size() + " — " + b.displayName), panelX + 120, panelY + 88, b.activeNow() ? 0xFF72F5A5 : 0xFFFFB070);
        } else if (tab == 2 && state.products != null && !state.products.isEmpty()) {
            ProductState p = state.products.get(Math.floorMod(productIndex, state.products.size()));
            c.drawTextWithShadow(textRenderer, Text.literal((productIndex + 1) + "/" + state.products.size() + "  " + p.category() + " — " + p.item()), panelX + 125, panelY + 98, 0xFFFFD8F1);
        } else if (tab == 3 && state.holograms != null) {
            c.drawTextWithShadow(textRenderer, Text.literal("Hologramas cargados: " + state.holograms.size()), panelX + 125, panelY + 94, 0xFFFFD8F1);
        } else if (tab == 4) drawAudit(c);
        if (!serverMessage.isBlank()) c.drawTextWithShadow(textRenderer, Text.literal(serverMessage), panelX + 20, panelY + panelH - 28, 0xFFFFFFFF);
        super.render(c, mouseX, mouseY, delta);
    }

    private void drawAudit(DrawContext c) {
        c.drawTextWithShadow(textRenderer, Text.literal("Últimas transacciones autoritativas — servidor " + state.serverTime), panelX + 28, panelY + 88, 0xFFFFD8F1);
        List<String> lines = state.audit == null ? List.of() : state.audit;
        int first = Math.max(0, lines.size() - Math.min(24, (panelH - 150) / 12));
        int y = panelY + 110;
        for (int i = first; i < lines.size(); i++, y += 12) {
            String line = textRenderer.trimToWidth(lines.get(i), panelW - 56);
            c.drawText(textRenderer, Text.literal(line), panelX + 28, y, 0xFFCDB8CA, false);
        }
    }

    @Override public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) { }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    private void clientMessage(String value) { if (client != null && client.player != null) client.player.sendMessage(Text.literal(value), false); }
    private String value(int i) { return fields.get(i).field.getText(); }
    private int integer(int i) { return Integer.parseInt(value(i)); }
    private long longValue(int i) { return Long.parseLong(value(i)); }
    private double decimal(int i) { return Double.parseDouble(value(i).replace(',', '.')); }
    private static String number(double v) { return String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static String escape(String v) { return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
    private static String joinInts(List<Integer> v) { return v == null ? "" : String.join(",", v.stream().map(String::valueOf).toList()); }
    private static String joinMap(java.util.Map<String, Double> v) { if (v == null) return ""; return String.join(",", v.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(e -> e.getKey() + "=" + number(e.getValue())).toList()); }
    private static List<Integer> parseInts(String v) { if (v.isBlank()) return new ArrayList<>(); List<Integer> out = new ArrayList<>(); for (String s : v.split(",")) out.add(Integer.parseInt(s.strip())); return out; }
    private static HashMap<String, Double> parseMap(String v) { HashMap<String, Double> out = new HashMap<>(); if (v.isBlank()) return out; for (String p : v.split(",")) { String[] pair = p.strip().split("=", 2); if (pair.length != 2) throw new IllegalArgumentException("usa clave=valor"); out.put(pair[0].strip(), Double.parseDouble(pair[1].strip().replace(',', '.'))); } return out; }
    private record LabeledField(String label, TextFieldWidget field) { }
}
