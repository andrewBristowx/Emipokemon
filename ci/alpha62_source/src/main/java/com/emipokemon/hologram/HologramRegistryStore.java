package com.emipokemon.hologram;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Small server-side persistent index for holograms. It deliberately uses only JDK IO so the
 * registry remains independent from client-only classes and optional integrations such as Streamotes.
 */
public final class HologramRegistryStore {
    private static final Path FILE = Path.of("config", "emipokemon", "holograms-v1.properties");
    private static final Properties DATA = new Properties();
    private static boolean loaded;

    private HologramRegistryStore() {}

    public record Entry(String id, String text, float scale, int color, String world,
                        double x, double y, double z) {
        public Entry {
            id = normalize(id);
            text = text == null ? "" : text;
            scale = Math.max(0.25F, Math.min(8.0F, scale));
            color |= 0xFF000000;
            world = world == null ? "minecraft:overworld" : world;
        }
    }

    public static synchronized Entry get(String rawId) {
        load();
        String id = normalize(rawId);
        if (id.isBlank() || !DATA.containsKey(key(id, "world"))) return null;
        try {
            return new Entry(id,
                    decode(DATA.getProperty(key(id, "text"), "")),
                    Float.parseFloat(DATA.getProperty(key(id, "scale"), "1.0")),
                    Integer.parseUnsignedInt(DATA.getProperty(key(id, "color"), "FFFFFFFF"), 16),
                    DATA.getProperty(key(id, "world"), "minecraft:overworld"),
                    Double.parseDouble(DATA.getProperty(key(id, "x"), "0")),
                    Double.parseDouble(DATA.getProperty(key(id, "y"), "0")),
                    Double.parseDouble(DATA.getProperty(key(id, "z"), "0")));
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    public static synchronized boolean contains(String id) {
        return get(id) != null;
    }

    public static synchronized List<Entry> all() {
        load();
        List<Entry> result = new ArrayList<>();
        for (String name : DATA.stringPropertyNames()) {
            if (!name.endsWith(".world")) continue;
            Entry entry = get(name.substring(0, name.length() - ".world".length()));
            if (entry != null) result.add(entry);
        }
        result.sort(Comparator.comparing(Entry::id));
        return result;
    }

    public static synchronized void put(Entry entry) {
        load();
        if (entry == null || entry.id().isBlank()) throw new IllegalArgumentException("hologram id");
        String id = entry.id();
        DATA.setProperty(key(id, "world"), entry.world());
        DATA.setProperty(key(id, "x"), Double.toString(entry.x()));
        DATA.setProperty(key(id, "y"), Double.toString(entry.y()));
        DATA.setProperty(key(id, "z"), Double.toString(entry.z()));
        DATA.setProperty(key(id, "text"), encode(entry.text()));
        DATA.setProperty(key(id, "scale"), Float.toString(entry.scale()));
        DATA.setProperty(key(id, "color"), String.format(Locale.ROOT, "%08X", entry.color()));
        save();
    }

    public static synchronized boolean remove(String rawId) {
        load();
        String id = normalize(rawId);
        boolean changed = false;
        for (String suffix : List.of("world", "x", "y", "z", "text", "scale", "color")) {
            changed |= DATA.remove(key(id, suffix)) != null;
        }
        if (changed) save();
        return changed;
    }

    static String normalize(String value) {
        if (value == null) return "";
        String id = value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return id.length() > 32 ? id.substring(0, 32) : id;
    }

    private static String key(String id, String suffix) { return id + "." + suffix; }
    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(FILE)) return;
        try (InputStream in = Files.newInputStream(FILE)) {
            DATA.load(in);
        } catch (IOException ignored) {
            // Keep an empty registry; the service will re-index any loaded entity it can see.
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temp)) {
                DATA.store(out, "Emipokemon hologram registry v1");
            }
            try {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar " + FILE, exception);
        }
    }
}
