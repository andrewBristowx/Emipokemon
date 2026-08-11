from pathlib import Path
import json

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha31 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.30','mod_version=0.4.0-alpha.31','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.30','0.4.0-alpha.31','core version')
s=replace_once(s,'import com.emipokemon.hologram.HologramService;','import com.emipokemon.hologram.HologramService;\nimport com.emipokemon.hologram.HologramViewerTextService;','viewer service import')
s=replace_once(s,'        HologramCommands.register();\n','        HologramCommands.register();\n        HologramViewerTextService.initialize();\n','viewer service init')
write(p,s)

# DecentHolograms-style architecture: resolve viewer-dependent text before sending it.
# The previous renderer mixin remains in source only for history but is not loaded in alpha.31.
p='src/client/resources/emipokemon.client.mixins.json'; obj=json.loads(read(p))
obj['client']=[name for name in obj.get('client',[]) if name != 'TextDisplayHologramMixin']
write(p,json.dumps(obj,indent=2,ensure_ascii=False)+'\n')

# Access vanilla TextDisplayEntity.TEXT so the server can send a different metadata value to each viewer.
p='src/main/resources/emipokemon.mixins.json'; obj=json.loads(read(p))
if 'TextDisplayTrackedDataAccessor' not in obj.get('mixins',[]):
    obj.setdefault('mixins',[]).append('TextDisplayTrackedDataAccessor')
write(p,json.dumps(obj,indent=2,ensure_ascii=False)+'\n')

write('src/main/java/com/emipokemon/mixin/TextDisplayTrackedDataAccessor.java', '''package com.emipokemon.mixin;\n\nimport net.minecraft.entity.data.TrackedData;\nimport net.minecraft.entity.decoration.DisplayEntity;\nimport net.minecraft.text.Text;\nimport org.spongepowered.asm.mixin.Mixin;\nimport org.spongepowered.asm.mixin.gen.Accessor;\n\n/** Exposes vanilla TextDisplayEntity's tracked text slot for per-viewer metadata packets. */\n@Mixin(DisplayEntity.TextDisplayEntity.class)\npublic interface TextDisplayTrackedDataAccessor {\n    @Accessor("TEXT")\n    static TrackedData<Text> emipokemon$getTextTrackedData() {\n        throw new AssertionError();\n    }\n}\n''')

write('src/main/java/com/emipokemon/hologram/HologramViewerTextService.java', r'''package com.emipokemon.hologram;

import com.emipokemon.Emipokemon;
import com.emipokemon.mixin.TextDisplayTrackedDataAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends viewer-specific TextDisplayEntity metadata without mutating the authoritative entity.
 *
 * DecentHolograms resolves placeholders for each viewer before sending entity metadata. Emipokemon
 * uses the same architecture while retaining the vanilla minecraft:text_display runtime entity.
 */
public final class HologramViewerTextService {
    private static final Pattern EMOTE = Pattern.compile(":([A-Za-z0-9_]{1,64}):");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<CacheKey, CacheValue> LAST_SENT = new HashMap<>();
    private static boolean initialized;
    private static long ticks;

    private HologramViewerTextService() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(HologramViewerTextService::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_SENT.clear();
            ticks = 0L;
        });
    }

    private static void tick(MinecraftServer server) {
        ticks++;
        if ((ticks % 10L) != 0L) return;

        Set<CacheKey> active = new HashSet<>();
        for (DisplayEntity.TextDisplayEntity display : HologramService.all(server)) {
            String hologramId = hologramId(display);
            if (hologramId == null) continue;
            HologramRegistryStore.Entry entry = HologramRegistryStore.get(hologramId);
            if (entry == null) continue;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getWorld() != display.getWorld()) continue;
                if (player.squaredDistanceTo(display) > (96.0D * 96.0D)) continue;

                CacheKey key = new CacheKey(player.getUuid(), display.getId());
                active.add(key);
                Text viewerText = viewerText(server, player, entry);
                String fingerprint = entry.text() + '\\u0000' + viewerText.getString() + '\\u0000' + entry.color();
                CacheValue previous = LAST_SENT.get(key);
                if (previous != null && previous.fingerprint().equals(fingerprint)
                        && ticks - previous.tick() < 100L) {
                    continue;
                }

                var trackedText = TextDisplayTrackedDataAccessor.emipokemon$getTextTrackedData();
                var value = DataTracker.SerializedEntry.of(trackedText, viewerText);
                player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(
                        display.getId(), java.util.List.of(value)));
                LAST_SENT.put(key, new CacheValue(fingerprint, ticks));
            }
        }

        if ((ticks % 1200L) == 0L) LAST_SENT.keySet().retainAll(active);
    }

    private static Text viewerText(MinecraftServer server, ServerPlayerEntity player,
                                   HologramRegistryStore.Entry entry) {
        String resolved = placeholders(server, player, entry.text(), entry.id());
        Style base = Style.EMPTY.withColor(entry.color() & 0xFFFFFF);
        Matcher matcher = EMOTE.matcher(resolved);
        if (!matcher.find()) return Text.literal(resolved).setStyle(base);

        matcher.reset();
        MutableText result = Text.empty().setStyle(base);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                result.append(Text.literal(resolved.substring(cursor, matcher.start())).setStyle(base));
            }
            String name = matcher.group(1);
            // Streamotes 1.2.12 uses COPY_TO_CLIPBOARD(name) as its vanilla Style marker.
            // Its font mixin turns the name into an emote glyph if that emote exists locally.
            Style emoteStyle = base.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name));
            result.append(Text.literal(name).setStyle(emoteStyle));
            cursor = matcher.end();
        }
        if (cursor < resolved.length()) {
            result.append(Text.literal(resolved.substring(cursor)).setStyle(base));
        }
        return result;
    }

    private static String placeholders(MinecraftServer server, ServerPlayerEntity player,
                                       String raw, String hologramId) {
        BlockPos pos = player.getBlockPos();
        LocalDateTime now = LocalDateTime.now();
        String biome = player.getWorld().getBiome(pos).getKey()
                .map(key -> key.getValue().toString()).orElse("desconocido");

        Map<String, String> values = Map.ofEntries(
                Map.entry("{player}", player.getName().getString()),
                Map.entry("{displayname}", player.getDisplayName().getString()),
                Map.entry("{uuid}", player.getUuidAsString()),
                Map.entry("{michicoins}", Long.toString(Emipokemon.progressionService().balance(player.getUuid()))),
                Map.entry("{ping}", Integer.toString(player.networkHandler.getLatency())),
                Map.entry("{fps}", "?"),
                Map.entry("{online}", Integer.toString(server.getPlayerManager().getCurrentPlayerCount())),
                Map.entry("{max_players}", Integer.toString(server.getPlayerManager().getMaxPlayerCount())),
                Map.entry("{server}", server.getServerMotd()),
                Map.entry("{dimension}", player.getWorld().getRegistryKey().getValue().toString()),
                Map.entry("{biome}", biome),
                Map.entry("{x}", Integer.toString(pos.getX())),
                Map.entry("{y}", Integer.toString(pos.getY())),
                Map.entry("{z}", Integer.toString(pos.getZ())),
                Map.entry("{time}", TIME.format(now)),
                Map.entry("{date}", DATE.format(now)),
                Map.entry("{hologram_id}", hologramId == null ? "" : hologramId),
                Map.entry("{emipokemon_version}", Emipokemon.VERSION)
        );
        String result = raw == null ? "" : raw;
        for (Map.Entry<String, String> value : values.entrySet()) {
            result = result.replace(value.getKey(), value.getValue());
        }
        return result;
    }

    private static String hologramId(DisplayEntity.TextDisplayEntity display) {
        for (String tag : display.getCommandTags()) {
            if (tag.startsWith(VanillaTextHologram.TAG_PREFIX)
                    && tag.length() > VanillaTextHologram.TAG_PREFIX.length()) {
                return tag.substring(VanillaTextHologram.TAG_PREFIX.length()).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private record CacheKey(UUID playerId, int entityId) {}
    private record CacheValue(String fingerprint, long tick) {}
}
''')

# Regression coverage: server-side per-viewer metadata + Streamotes style marker + no renderer mixin.
p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha30VersionIsConsistentInSource','alpha31VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.30','0.4.0-alpha.31')
insert='''\n    @Test\n    void alpha31ResolvesViewerTextBeforeSendingMetadata() throws Exception {\n        String viewer = source("main/java/com/emipokemon/hologram/HologramViewerTextService.java");\n        String commonMixins = source("main/resources/emipokemon.mixins.json");\n        String clientMixins = source("client/resources/emipokemon.client.mixins.json");\n        assertTrue(viewer.contains("EntityTrackerUpdateS2CPacket"));\n        assertTrue(viewer.contains("DataTracker.SerializedEntry.of"));\n        assertTrue(viewer.contains("{player}"));\n        assertTrue(viewer.contains("ClickEvent.Action.COPY_TO_CLIPBOARD"));\n        assertTrue(commonMixins.contains("TextDisplayTrackedDataAccessor"));\n        assertFalse(clientMixins.contains("TextDisplayHologramMixin"));\n    }\n'''
pos=s.rfind('\n}')
if pos < 0: raise SystemExit('missing alpha31 patch anchor: test class closing brace')
s=s[:pos]+insert+s[pos:]
write(p,s)

write('CHANGELOG-0.4.0-alpha.31.md','''# Emipokemon 0.4.0-alpha.31\n\n- Mantiene `minecraft:text_display` y `see_through=false`, ya validados visualmente.\n- Cambia placeholders a resolución por jugador en servidor, siguiendo la arquitectura útil de DecentHolograms.\n- Envía `Text` individual mediante metadata de la entidad sin mutar el estado autoritativo global.\n- `{player}` y demás placeholders de servidor ya no dependen del mixin de renderer cliente.\n- Streamotes usa el mismo marcador de Style que Streamotes 1.2.12: `COPY_TO_CLIPBOARD(nombre)`.\n- Si Streamotes falta o el emote no existe, el nombre queda como texto normal.\n- El botón `✦ Emotes` conserva el comportamiento solo-ratón de alpha.30.\n- `{fps}` devuelve `?` porque el servidor no conoce los FPS del cliente.\n\nPendiente de validación visual real en Cobbleverse.\n''')
