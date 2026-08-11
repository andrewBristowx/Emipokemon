from pathlib import Path
import re

root = Path('.')

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'missing alpha32 patch anchor: {label}')
    return text.replace(old, new, 1)

# Version
p='gradle.properties'; s=read(p); s=replace_once(s,'mod_version=0.4.0-alpha.31','mod_version=0.4.0-alpha.32','gradle version'); write(p,s)
p='src/main/java/com/emipokemon/Emipokemon.java'; s=read(p); s=replace_once(s,'0.4.0-alpha.31','0.4.0-alpha.32','core version'); write(p,s)

# Keep per-viewer placeholders on the server, but preserve :emote: tokens unchanged.
# Streamotes' registry and texture state are client-only, so alpha.32 resolves emotes locally.
p='src/main/java/com/emipokemon/hologram/HologramViewerTextService.java'; s=read(p)
start=s.index('    private static Text viewerText(')
end=s.index('    private static String placeholders(', start)
viewer='''    private static Text viewerText(MinecraftServer server, ServerPlayerEntity player,\n                                   HologramRegistryStore.Entry entry) {\n        String resolved = placeholders(server, player, entry.text(), entry.id());\n        Style base = Style.EMPTY.withColor(entry.color() & 0xFFFFFF);\n        return Text.literal(resolved).setStyle(base);\n    }\n\n'''
s=s[:start]+viewer+s[end:]
write(p,s)

# Resolve Streamotes on the client where EmoticonRegistry and textures actually exist.
write('src/client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java', r'''package com.emipokemon.client.emote;

import com.emipokemon.mixin.TextDisplayTrackedDataAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-local Streamotes bridge for vanilla text displays.
 *
 * The server resolves viewer-specific placeholders but intentionally leaves :emote: tokens intact.
 * This service consults the real client-side Streamotes registry and applies Streamotes' own Style
 * returned by Compat.makeEmoteStyle. If Streamotes or the named emote is unavailable, the token is
 * left untouched and ordinary hologram text continues to work.
 */
public final class HologramStreamotesClientService {
    private static final Pattern EMOTE = Pattern.compile(":([^\\s:]{1,64}):");
    private static boolean initialized;
    private static boolean bridgeAttempted;
    private static Method fromName;
    private static Method makeEmoteStyle;

    private HologramStreamotesClientService() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(HologramStreamotesClientService::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) return;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity display)) continue;
            var trackedText = TextDisplayTrackedDataAccessor.emipokemon$getTextTrackedData();
            Text current = display.getDataTracker().get(trackedText);
            Text resolved = resolve(current);
            if (resolved != current && !resolved.equals(current)) {
                display.getDataTracker().set(trackedText, resolved, true);
            }
        }
    }

    static Text resolve(Text original) {
        if (original == null) return Text.empty();
        String raw = original.getString();
        Matcher matcher = EMOTE.matcher(raw);
        if (!matcher.find()) return original;

        Style base = original.getStyle();
        matcher.reset();
        MutableText out = Text.empty().setStyle(base);
        int cursor = 0;
        boolean changed = false;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                out.append(Text.literal(raw.substring(cursor, matcher.start())).setStyle(base));
            }
            String name = matcher.group(1);
            Style emoteStyle = lookupStyle(name, base);
            if (emoteStyle == null) {
                out.append(Text.literal(matcher.group()).setStyle(base));
            } else {
                out.append(Text.literal(name).setStyle(emoteStyle));
                changed = true;
            }
            cursor = matcher.end();
        }
        if (cursor < raw.length()) {
            out.append(Text.literal(raw.substring(cursor)).setStyle(base));
        }
        return changed ? out : original;
    }

    private static Style lookupStyle(String name, Style base) {
        try {
            ensureBridge();
            if (fromName == null || makeEmoteStyle == null) return null;
            Object emote = fromName.invoke(null, name);
            if (emote == null) return null;
            Object style = makeEmoteStyle.invoke(null, emote);
            if (style instanceof Style streamotesStyle) {
                return streamotesStyle.withParent(base);
            }
        } catch (Throwable ignored) {
            // Streamotes is optional. Never break ordinary hologram text.
        }
        return null;
    }

    private static synchronized void ensureBridge() {
        if (bridgeAttempted) return;
        bridgeAttempted = true;
        try {
            Class<?> registry = Class.forName("xeed.mc.streamotes.emoticon.EmoticonRegistry");
            Class<?> emote = Class.forName("xeed.mc.streamotes.emoticon.Emoticon");
            Class<?> compat = Class.forName("xeed.mc.streamotes.Compat");
            fromName = registry.getMethod("fromName", String.class);
            makeEmoteStyle = compat.getMethod("makeEmoteStyle", emote);
        } catch (Throwable ignored) {
            fromName = null;
            makeEmoteStyle = null;
        }
    }
}
''')

# Initialize the client service without relying on a renderer mixin.
p='src/client/java/com/emipokemon/EmipokemonClient.java'; s=read(p)
if 'HologramStreamotesClientService' not in s:
    package_end=s.index('\n', s.index('package ')) + 1
    s=s[:package_end]+'\nimport com.emipokemon.client.emote.HologramStreamotesClientService;'+s[package_end:]
needle='    public void onInitializeClient() {'
s=replace_once(s, needle, needle+'\n        HologramStreamotesClientService.initialize();', 'client initializer')
write(p,s)

# Update source regression expectations from alpha.31 and add alpha.32 coverage.
p='src/test/java/com/emipokemon/visual/VisualRefreshRegressionTest.java'; s=read(p)
s=s.replace('alpha31VersionIsConsistentInSource','alpha32VersionIsConsistentInSource')
s=s.replace('0.4.0-alpha.31','0.4.0-alpha.32')
s=s.replace('        assertTrue(viewer.contains("ClickEvent.Action.COPY_TO_CLIPBOARD"));\n','        assertTrue(viewer.contains("return Text.literal(resolved).setStyle(base);"));\n')
insert='''\n    @Test\n    void alpha32ResolvesStreamotesFromClientRegistry() throws Exception {\n        String client = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");\n        assertTrue(client.contains("EmoticonRegistry"));\n        assertTrue(client.contains("Compat"));\n        assertTrue(client.contains("makeEmoteStyle"));\n        assertTrue(client.contains("client.world.getEntities()"));\n        assertTrue(client.contains("display.getDataTracker().set(trackedText, resolved, true)"));\n        assertTrue(client.contains("return changed ? out : original"));\n    }\n'''
pos=s.rfind('\n}')
if pos < 0: raise SystemExit('missing alpha32 test class closing brace')
s=s[:pos]+insert+s[pos:]
write(p,s)

write('CHANGELOG-0.4.0-alpha.32.md','''# Emipokemon 0.4.0-alpha.32\n\n- Mantiene placeholders por jugador y `minecraft:text_display` de alpha.31.\n- Mantiene `see_through=false`, ya validado visualmente.\n- El servidor ya no intenta fabricar el Style de Streamotes. Conserva `:emote:` tras resolver placeholders.\n- El cliente consulta directamente `EmoticonRegistry.fromName` y usa `Compat.makeEmoteStyle` del Streamotes realmente instalado.\n- El TextDisplay se actualiza solo localmente con el Style retornado por Streamotes.\n- Si Streamotes falta, todavía carga o el emote no existe, el token `:emote:` queda como texto normal y no rompe el holograma.\n- Conserva el botón de emotes solo-ratón.\n\nPendiente de validación visual real en Cobbleverse.\n''')
