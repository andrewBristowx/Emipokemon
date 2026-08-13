package com.emipokemon.client.emote;

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

    public static Text resolve(Text original) {
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
