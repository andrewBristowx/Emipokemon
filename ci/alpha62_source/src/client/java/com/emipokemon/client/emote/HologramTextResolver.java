package com.emipokemon.client.emote;

import com.emipokemon.Emipokemon;
import com.emipokemon.client.progress.ProgressionClient;
import com.emipokemon.hologram.HologramEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves viewer-local placeholders and Streamotes glyphs without trusting client input. */
public final class HologramTextResolver {
    private static final String CUSTOM_NAME_PREFIX = "Emipokemon hologram ";
    private static final String TEXT_MARKER_PREFIX = "emipokemon:hologram-text:";
    private static final Pattern EMOTE = Pattern.compile(":([A-Za-z0-9_]{1,64}):");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(?:player|displayname|uuid|michicoins|ping|fps|online|max_players|server|dimension|biome|x|y|z|time|date|hologram_id|emipokemon_version)\\}");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<String, EmoteEntry> EMOTES = new HashMap<>();
    private static int emoteRevision = -1;

    private HologramTextResolver() {
    }

    public static Text resolve(HologramEntity hologram) {
        return resolveText(hologram.hologramText(), hologram.hologramId());
    }

    /** Resolve a vanilla text_display hologram immediately before Minecraft renders it. */
    public static Text resolve(DisplayEntity.TextDisplayEntity display, Text serverText) {
        if (display == null || serverText == null) return serverText == null ? Text.empty() : serverText;
        String id = hologramId(display, serverText);
        if (id == null && !shouldResolve(serverText)) return serverText;
        return resolveText(serverText, id == null ? "" : id);
    }

    private static Text resolveText(Text serverText, String hologramId) {
        String resolved = placeholders(serverText.getString(), hologramId);
        Matcher matcher = EMOTE.matcher(resolved);
        // Plain text and placeholders must not depend on the optional Streamotes API at all.
        // This also keeps ordinary holograms visible while Streamotes is absent or still loading.
        if (!matcher.find()) return Text.literal(resolved).setStyle(serverText.getStyle());
        matcher.reset();
        refreshEmotes();
        MutableText result = Text.empty();
        int cursor = 0;
        List<EmoteEntry> requested = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                result.append(Text.literal(resolved.substring(cursor, matcher.start())).setStyle(serverText.getStyle()));
            }
            String requestedName = matcher.group(1);
            EmoteEntry entry = EMOTES.get(requestedName.toLowerCase(Locale.ROOT));
            if (entry == null) entry = StreamotesBridge.lookup(requestedName);
            if (entry == null) {
                result.append(Text.literal(matcher.group()).setStyle(serverText.getStyle()));
            } else {
                result.append(Text.literal(entry.name()).setStyle(entry.style()));
                requested.add(entry);
            }
            cursor = matcher.end();
        }
        if (cursor < resolved.length()) {
            result.append(Text.literal(resolved.substring(cursor)).setStyle(serverText.getStyle()));
        }
        if (!requested.isEmpty()) EmotePreviewCache.enqueue(requested);
        return result;
    }

    public static boolean isEmipokemonDisplay(DisplayEntity.TextDisplayEntity display, Text serverText) {
        return display != null && serverText != null
                && (hologramId(display, serverText) != null || shouldResolve(serverText));
    }

    public static boolean shouldResolve(Text serverText) {
        if (serverText == null) return false;
        String raw = serverText.getString();
        return PLACEHOLDER.matcher(raw).find() || EMOTE.matcher(raw).find();
    }

    private static String hologramId(DisplayEntity.TextDisplayEntity display, Text serverText) {
        String insertion = serverText.getStyle().getInsertion();
        if (insertion != null && insertion.startsWith(TEXT_MARKER_PREFIX)
                && insertion.length() > TEXT_MARKER_PREFIX.length()) {
            return insertion.substring(TEXT_MARKER_PREFIX.length()).strip().toLowerCase(Locale.ROOT);
        }
        for (Text sibling : serverText.getSiblings()) {
            String siblingInsertion = sibling.getStyle().getInsertion();
            if (siblingInsertion != null && siblingInsertion.startsWith(TEXT_MARKER_PREFIX)
                    && siblingInsertion.length() > TEXT_MARKER_PREFIX.length()) {
                return siblingInsertion.substring(TEXT_MARKER_PREFIX.length()).strip().toLowerCase(Locale.ROOT);
            }
        }
        Text customName = display.getCustomName();
        if (customName != null) {
            String value = customName.getString();
            if (value.startsWith(CUSTOM_NAME_PREFIX) && value.length() > CUSTOM_NAME_PREFIX.length()) {
                return value.substring(CUSTOM_NAME_PREFIX.length()).strip().toLowerCase(Locale.ROOT);
            }
        }
        final String prefix = "emipokemon:hologram:";
        for (String tag : display.getCommandTags()) {
            if (tag.startsWith(prefix) && tag.length() > prefix.length()) return tag.substring(prefix.length());
        }
        return null;
    }

    private static void refreshEmotes() {
        List<EmoteEntry> entries = StreamotesCatalog.all();
        int revision = StreamotesCatalog.revision();
        if (revision == emoteRevision) return;
        EMOTES.clear();
        for (EmoteEntry entry : entries) {
            EMOTES.putIfAbsent(entry.name().toLowerCase(Locale.ROOT), entry);
        }
        emoteRevision = revision;
    }

    private static String placeholders(String raw, HologramEntity hologram) {
        return placeholders(raw, hologram.hologramId());
    }

    private static String placeholders(String raw, String hologramId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return raw;
        LocalDateTime now = LocalDateTime.now();
        BlockPos pos = client.player.getBlockPos();
        PlayerListEntry entry = client.getNetworkHandler() == null ? null
                : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        int online = client.getNetworkHandler() == null ? 1 : client.getNetworkHandler().getPlayerList().size();
        int maxPlayers = reflectedMaxPlayers(client, online);
        String server = client.getCurrentServerEntry() == null ? "Mundo local" : client.getCurrentServerEntry().address;
        String biome = client.world.getBiome(pos).getKey().map(key -> key.getValue().toString()).orElse("desconocido");
        long balance = ProgressionClient.cachedBalanceValue();
        Map<String, String> values = Map.ofEntries(
                Map.entry("{player}", client.player.getName().getString()),
                Map.entry("{displayname}", client.player.getDisplayName().getString()),
                Map.entry("{uuid}", client.player.getUuidAsString()),
                Map.entry("{michicoins}", balance < 0 ? "…" : Long.toString(balance)),
                Map.entry("{ping}", entry == null ? "?" : Integer.toString(entry.getLatency())),
                Map.entry("{fps}", Integer.toString(client.getCurrentFps())),
                Map.entry("{online}", Integer.toString(online)),
                Map.entry("{max_players}", Integer.toString(maxPlayers)),
                Map.entry("{server}", server),
                Map.entry("{dimension}", client.world.getRegistryKey().getValue().toString()),
                Map.entry("{biome}", biome),
                Map.entry("{x}", Integer.toString(pos.getX())),
                Map.entry("{y}", Integer.toString(pos.getY())),
                Map.entry("{z}", Integer.toString(pos.getZ())),
                Map.entry("{time}", TIME.format(now)),
                Map.entry("{date}", DATE.format(now)),
                Map.entry("{hologram_id}", hologramId == null ? "" : hologramId),
                Map.entry("{emipokemon_version}", Emipokemon.VERSION)
        );
        String result = raw;
        for (Map.Entry<String, String> value : values.entrySet()) result = result.replace(value.getKey(), value.getValue());
        return result;
    }

    private static int reflectedMaxPlayers(MinecraftClient client, int fallback) {
        Object serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) return fallback;
        try {
            Object players = serverInfo.getClass().getField("players").get(serverInfo);
            if (players == null) return fallback;
            for (String name : List.of("max", "maxPlayers", "getMax")) {
                try {
                    Object value = players.getClass().getMethod(name).invoke(players);
                    if (value instanceof Number number) return Math.max(fallback, number.intValue());
                } catch (ReflectiveOperationException ignored) {
                    // Yarn and production mappings expose this optional record differently.
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // The online count remains a safe fallback when the server list has no capacity metadata.
        }
        return fallback;
    }
}
