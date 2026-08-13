package com.emipokemon.client.visual;

import com.emipokemon.Emipokemon;
import com.emipokemon.visual.VisualAssetNetworking.AssetChunkPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ClientVisualAssetCache {
    private static final Map<String, Assembly> ASSEMBLIES = new HashMap<>();
    private static final Map<String, AssetFrames> ASSETS = new HashMap<>();

    private ClientVisualAssetCache() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(AssetChunkPayload.ID, (payload, context) ->
                context.client().execute(() -> accept(payload)));
    }

    public static Identifier texture(String key, Identifier fallback) {
        AssetFrames asset = ASSETS.get(key);
        return asset == null ? fallback : asset.currentTexture();
    }

    private static void accept(AssetChunkPayload payload) {
        if (payload.key() == null || payload.hash() == null || payload.total() < 1 || payload.total() > 300
                || payload.index() < 0 || payload.index() >= payload.total()) return;
        AssetFrames existing = ASSETS.get(payload.key());
        if (existing != null && existing.hash.equals(payload.hash())) return;
        String assemblyKey = payload.key() + "@" + payload.hash();
        Assembly assembly = ASSEMBLIES.computeIfAbsent(assemblyKey,
                ignored -> new Assembly(payload.key(), payload.mediaType(), payload.hash(), payload.total()));
        if (assembly.total != payload.total() || !assembly.mediaType.equals(payload.mediaType())) return;
        try {
            byte[] part = Base64.getDecoder().decode(payload.base64Data());
            if (part.length > 18_000) return;
            assembly.parts[payload.index()] = part;
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (!assembly.complete()) return;
        ASSEMBLIES.remove(assemblyKey);
        byte[] bytes = assembly.join();
        if (bytes.length > 4 * 1024 * 1024) return;
        CompletableFuture.supplyAsync(() -> decode(bytes, assembly.mediaType))
                .thenAccept(decoded -> MinecraftClient.getInstance().execute(() -> register(assembly, decoded)))
                .exceptionally(error -> {
                    Emipokemon.LOGGER.warn("Could not decode visual asset {}: {}", assembly.key, error.getMessage());
                    return null;
                });
    }

    private static Decoded decode(byte[] bytes, String type) {
        try {
            if ("png".equals(type)) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null) throw new IllegalArgumentException("Invalid PNG");
                return new Decoded(List.of(toNative(image)), new int[]{1000});
            }
            List<NativeImage> frames = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) throw new IllegalArgumentException("GIF reader unavailable");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, false, false);
                    int count = reader.getNumImages(true);
                    for (int index = 0; index < count; index++) {
                        frames.add(toNative(reader.read(index)));
                        delays.add(gifDelay(reader.getImageMetadata(index)));
                    }
                } finally {
                    reader.dispose();
                }
            }
            if (frames.isEmpty()) throw new IllegalArgumentException("Empty GIF");
            return new Decoded(frames, delays.stream().mapToInt(Integer::intValue).toArray());
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static NativeImage toNative(BufferedImage source) {
        NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int a = (argb >>> 24) & 0xff;
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;
                // NativeImage stores pixels as ABGR, while BufferedImage returns ARGB.
                image.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return image;
    }

    private static int gifDelay(IIOMetadata metadata) {
        try {
            org.w3c.dom.Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            return findDelay(root);
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static int findDelay(org.w3c.dom.Node node) {
        if ("GraphicControlExtension".equals(node.getNodeName()) && node.getAttributes() != null) {
            org.w3c.dom.Node delay = node.getAttributes().getNamedItem("delayTime");
            if (delay != null) return Math.max(20, Integer.parseInt(delay.getNodeValue()) * 10);
        }
        for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            int result = findDelay(child);
            if (result != -1) return result;
        }
        return -1;
    }

    private static void register(Assembly assembly, Decoded decoded) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Identifier> textures = new ArrayList<>();
        for (int index = 0; index < decoded.frames.size(); index++) {
            NativeImageBackedTexture texture = new NativeImageBackedTexture(decoded.frames.get(index));
            textures.add(client.getTextureManager().registerDynamicTexture(
                    "emipokemon/" + sanitize(assembly.key) + "/" + assembly.hash + "/" + index, texture));
        }
        AssetFrames previous = ASSETS.put(assembly.key,
                new AssetFrames(assembly.hash, textures, decoded.delays, System.currentTimeMillis()));
        if (previous != null) previous.close(client);
    }

    private static String sanitize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_");
    }

    private record Decoded(List<NativeImage> frames, int[] delays) {
    }

    private static final class Assembly {
        private final String key;
        private final String mediaType;
        private final String hash;
        private final int total;
        private final byte[][] parts;

        private Assembly(String key, String mediaType, String hash, int total) {
            this.key = key;
            this.mediaType = mediaType;
            this.hash = hash;
            this.total = total;
            this.parts = new byte[total][];
        }

        private boolean complete() {
            return Arrays.stream(parts).allMatch(java.util.Objects::nonNull);
        }

        private byte[] join() {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for (byte[] part : parts) output.write(part);
                return output.toByteArray();
            } catch (Exception exception) {
                return new byte[0];
            }
        }
    }

    private static final class AssetFrames {
        private final String hash;
        private final List<Identifier> textures;
        private final int[] delays;
        private final long startedAt;

        private AssetFrames(String hash, List<Identifier> textures, int[] delays, long startedAt) {
            this.hash = hash;
            this.textures = textures;
            this.delays = delays;
            this.startedAt = startedAt;
        }

        private Identifier currentTexture() {
            if (textures.size() == 1) return textures.getFirst();
            long cycle = 0L;
            for (int delay : delays) cycle += Math.max(20, delay);
            long cursor = Math.floorMod(System.currentTimeMillis() - startedAt, Math.max(1L, cycle));
            for (int index = 0; index < textures.size(); index++) {
                cursor -= Math.max(20, delays[Math.min(index, delays.length - 1)]);
                if (cursor < 0L) return textures.get(index);
            }
            return textures.getLast();
        }

        private void close(MinecraftClient client) {
            for (Identifier id : textures) client.getTextureManager().destroyTexture(id);
        }
    }
}
