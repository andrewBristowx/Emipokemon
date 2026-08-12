package com.emipokemon.hologram;

import com.emipokemon.registry.ModRegistries;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative hologram service.
 *
 * alpha.26 deliberately separates persistence from rendering: HologramRegistryStore owns the data,
 * while the runtime visual is Minecraft's vanilla text_display entity. HologramEntity is retained
 * only to migrate alpha.20-alpha.25 worlds and is never spawned for new holograms.
 */
public final class HologramService {
    private HologramService() {}

    public static DisplayEntity.TextDisplayEntity find(MinecraftServer server, String id) {
        if (server == null || id == null) return null;
        return findLoaded(server, id);
    }

    public static HologramRegistryStore.Entry record(MinecraftServer server, String id) {
        if (server == null || id == null) return null;
        migrateLegacyLoaded(server);
        return HologramRegistryStore.get(id);
    }

    public static boolean exists(MinecraftServer server, String id) {
        return record(server, id) != null;
    }

    public static List<HologramRegistryStore.Entry> records(MinecraftServer server) {
        migrateLegacyLoaded(server);
        return HologramRegistryStore.all();
    }

    public static List<DisplayEntity.TextDisplayEntity> all(MinecraftServer server) {
        if (server == null) return List.of();
        return allLoadedDisplays(server);
    }

    /** Restore persistent holograms with the vanilla text_display engine. Safe to call repeatedly. */
    public static void restoreAll(MinecraftServer server) {
        if (server == null) return;
        migrateLegacyLoaded(server);
        for (HologramRegistryStore.Entry entry : HologramRegistryStore.all()) {
            ServerWorld world = worldFor(server, entry.world());
            if (world == null) continue;

            DisplayEntity.TextDisplayEntity loaded = findLoaded(server, entry.id());
            if (loaded != null) {
                // alpha.29 replaces the loaded display once so the synchronized Text payload
                // definitely contains the invisible marker used by the client resolver. This
                // avoids relying on an in-place NBT reload being propagated to an already
                // tracked entity from alpha.26-alpha.28.
                loaded.discard();
            }
            VanillaTextHologram.spawn(world, entry);
        }
    }

    private static List<DisplayEntity.TextDisplayEntity> allLoadedDisplays(MinecraftServer server) {
        List<DisplayEntity.TextDisplayEntity> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            result.addAll(world.getEntitiesByType(EntityType.TEXT_DISPLAY,
                    entity -> hasEmipokemonTag(entity)));
        }
        return result;
    }

    private static DisplayEntity.TextDisplayEntity findLoaded(MinecraftServer server, String id) {
        for (ServerWorld world : server.getWorlds()) {
            var matches = world.getEntitiesByType(EntityType.TEXT_DISPLAY,
                    entity -> VanillaTextHologram.matches(entity, id));
            if (!matches.isEmpty()) return matches.getFirst();
        }
        return null;
    }

    private static boolean hasEmipokemonTag(DisplayEntity.TextDisplayEntity entity) {
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(VanillaTextHologram.TAG_PREFIX)) return true;
        }
        return false;
    }

    private static List<HologramEntity> legacyLoaded(MinecraftServer server) {
        List<HologramEntity> result = new ArrayList<>();
        for (ServerWorld world : server.getWorlds()) {
            result.addAll(world.getEntitiesByType(ModRegistries.HOLOGRAM, entity -> true));
        }
        return result;
    }

    /** Migrate alpha.20-alpha.25 custom entities into the persistent registry, then retire them. */
    public static void migrateLegacyLoaded(MinecraftServer server) {
        if (server == null) return;
        for (HologramEntity legacy : legacyLoaded(server)) {
            String id = legacy.hologramId();
            if (id == null || id.isBlank()) {
                legacy.discard();
                continue;
            }
            if (!HologramRegistryStore.contains(id)) HologramRegistryStore.put(entry(legacy));
            legacy.discard();
        }
    }

    public static HologramRegistryStore.Entry entry(HologramEntity entity) {
        return new HologramRegistryStore.Entry(entity.hologramId(), entity.hologramText().getString(),
                entity.hologramScale(), entity.hologramColor(), worldId(entity.getWorld()),
                entity.getX(), entity.getY(), entity.getZ());
    }

    public static DisplayEntity.TextDisplayEntity spawn(ServerWorld world, HologramRegistryStore.Entry entry) {
        return VanillaTextHologram.spawn(world, entry);
    }

    public static DisplayEntity.TextDisplayEntity create(ServerWorld world, String id, String text, Vec3d pos) {
        if (world == null || pos == null || id == null || id.isBlank()) return null;
        HologramRegistryStore.Entry entry = new HologramRegistryStore.Entry(id, text, 1.0F, 0xFFFFFFFF,
                worldId(world), pos.x, pos.y, pos.z);
        HologramRegistryStore.put(entry);
        DisplayEntity.TextDisplayEntity display = spawn(world, entry);
        if (display == null) HologramRegistryStore.remove(id);
        return display;
    }

    public static DisplayEntity.TextDisplayEntity move(MinecraftServer server, ServerWorld destination,
                                                        String id, Vec3d pos) {
        HologramRegistryStore.Entry current = record(server, id);
        if (current == null || destination == null || pos == null) return null;
        HologramRegistryStore.Entry moved = new HologramRegistryStore.Entry(current.id(), current.text(),
                current.scale(), current.color(), worldId(destination), pos.x, pos.y, pos.z);
        HologramRegistryStore.put(moved);
        discardDisplays(server, current.id());
        return spawn(destination, moved);
    }

    public static HologramRegistryStore.Entry updateText(MinecraftServer server, String id, String text) {
        HologramRegistryStore.Entry current = record(server, id);
        if (current == null) return null;
        HologramRegistryStore.Entry updated = new HologramRegistryStore.Entry(current.id(), text, current.scale(),
                current.color(), current.world(), current.x(), current.y(), current.z());
        HologramRegistryStore.put(updated);
        respawnIfPossible(server, updated);
        return updated;
    }

    public static HologramRegistryStore.Entry updateScale(MinecraftServer server, String id, float scale) {
        HologramRegistryStore.Entry current = record(server, id);
        if (current == null) return null;
        HologramRegistryStore.Entry updated = new HologramRegistryStore.Entry(current.id(), current.text(), scale,
                current.color(), current.world(), current.x(), current.y(), current.z());
        HologramRegistryStore.put(updated);
        // alpha.26 intentionally keeps vanilla display scale at 1.0 until plain text visibility is proven.
        return updated;
    }

    public static HologramRegistryStore.Entry updateColor(MinecraftServer server, String id, int color) {
        HologramRegistryStore.Entry current = record(server, id);
        if (current == null) return null;
        HologramRegistryStore.Entry updated = new HologramRegistryStore.Entry(current.id(), current.text(),
                current.scale(), color, current.world(), current.x(), current.y(), current.z());
        HologramRegistryStore.put(updated);
        respawnIfPossible(server, updated);
        return updated;
    }

    public static boolean remove(MinecraftServer server, String id) {
        migrateLegacyLoaded(server);
        boolean existed = HologramRegistryStore.remove(id);
        boolean loaded = discardDisplays(server, id);
        for (HologramEntity legacy : legacyLoaded(server)) {
            if (legacy.hologramId().equalsIgnoreCase(id)) {
                legacy.discard();
                loaded = true;
            }
        }
        return existed || loaded;
    }

    /** Legacy custom entities self-retire into the vanilla display system on their next server tick. */
    public static void reconcile(HologramEntity entity) {
        if (entity == null || entity.getWorld().isClient()) return;
        String id = entity.hologramId();
        if (id == null || id.isBlank()) {
            entity.discard();
            return;
        }
        if (!HologramRegistryStore.contains(id)) HologramRegistryStore.put(entry(entity));
        MinecraftServer server = entity.getServer();
        entity.discard();
        if (server != null) restoreAll(server);
    }

    public static String diagnostic(MinecraftServer server, String id) {
        HologramRegistryStore.Entry stored = record(server, id);
        DisplayEntity.TextDisplayEntity loaded = find(server, id);
        if (stored == null && loaded == null) return "persistido=no, text_display=no";
        if (stored == null) return "persistido=no, text_display=sí";
        return "persistido=sí, text_display=" + (loaded == null ? "no" : "sí")
                + ", motor=vanilla, mundo=" + stored.world() + ", pos="
                + block(stored.x()) + " " + block(stored.y()) + " " + block(stored.z());
    }

    private static void respawnIfPossible(MinecraftServer server, HologramRegistryStore.Entry entry) {
        ServerWorld world = worldFor(server, entry.world());
        if (world == null) return;
        discardDisplays(server, entry.id());
        spawn(world, entry);
    }

    private static boolean discardDisplays(MinecraftServer server, String id) {
        boolean found = false;
        if (server == null) return false;
        for (ServerWorld world : server.getWorlds()) {
            for (DisplayEntity.TextDisplayEntity display : world.getEntitiesByType(EntityType.TEXT_DISPLAY,
                    entity -> VanillaTextHologram.matches(entity, id))) {
                display.discard();
                found = true;
            }
        }
        return found;
    }

    private static ServerWorld worldFor(MinecraftServer server, String worldId) {
        if (server == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            if (worldId(world).equals(worldId)) return world;
        }
        return null;
    }

    private static String worldId(net.minecraft.world.World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static int block(double value) { return (int) Math.floor(value); }
}
