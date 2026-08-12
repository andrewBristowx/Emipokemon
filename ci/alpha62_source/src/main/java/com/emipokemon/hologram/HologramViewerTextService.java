package com.emipokemon.hologram;

import com.emipokemon.Emipokemon;
import com.emipokemon.mixin.TextDisplayTrackedDataAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
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
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof DisplayEntity.TextDisplayEntity display) {
                LAST_SENT.remove(new CacheKey(player.getUuid(), display.getId()));
            }
        });
        EntityTrackingEvents.STOP_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof DisplayEntity.TextDisplayEntity display) {
                LAST_SENT.remove(new CacheKey(player.getUuid(), display.getId()));
            }
        });
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
                String fingerprint = entry.text() + "|" + viewerText.getString() + "|" + entry.color();
                CacheValue previous = LAST_SENT.get(key);
                if (previous != null && previous.fingerprint().equals(fingerprint)) {
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
        return Text.literal(resolved).setStyle(base);
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
