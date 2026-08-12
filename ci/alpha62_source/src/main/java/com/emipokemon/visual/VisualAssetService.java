package com.emipokemon.visual;

import com.emipokemon.Emipokemon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class VisualAssetService {
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_DIMENSION = 2048;
    private static final int MAX_GIF_FRAMES = 160;
    private static final String NPC_README = "Coloca aquí skin.png (64x64 o 64x32) y ejecuta "
            + "/emipokemon npc skin <id> archivo\n";
    private static final String MEDIA_README = "Coloca aquí media.png o media.gif y ejecuta "
            + "/emipokemon media cargar <id> archivo\n";

    private final Path root;
    private final Path npcRoot;
    private final Path mediaRoot;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public VisualAssetService() {
        this(FabricLoader.getInstance().getConfigDir().resolve(Emipokemon.MOD_ID));
    }

    VisualAssetService(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.npcRoot = this.root.resolve("npcs");
        this.mediaRoot = this.root.resolve("media");
    }

    public void initialize() {
        try {
            Files.createDirectories(npcRoot);
            Files.createDirectories(mediaRoot);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not initialize visual asset folders", exception);
        }
    }

    public Path ensureNpcFolder(String id) throws Exception {
        Path folder = safeFolder(npcRoot, id);
        Files.createDirectories(folder);
        writeReadme(folder.resolve("LEEME.txt"), NPC_README);
        return folder;
    }

    public Path ensureMediaFolder(String id) throws Exception {
        Path folder = safeFolder(mediaRoot, id);
        Files.createDirectories(folder);
        writeReadme(folder.resolve("LEEME.txt"), MEDIA_README);
        return folder;
    }

    public Asset loadNpc(String id) throws Exception {
        Path file = ensureNpcFolder(id).resolve("skin.png");
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("No existe skin.png en " + file);
        requireSafeFileSize(file);
        return validate("npc:" + id, Files.readAllBytes(file), true, "png");
    }

    public Asset loadMedia(String id) throws Exception {
        Path folder = ensureMediaFolder(id);
        Path gif = folder.resolve("media.gif");
        Path png = folder.resolve("media.png");
        Path file = Files.isRegularFile(gif) ? gif : png;
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("No existe media.gif ni media.png en " + folder);
        requireSafeFileSize(file);
        String type = file.getFileName().toString().endsWith(".gif") ? "gif" : "png";
        return validate("media:" + id, Files.readAllBytes(file), false, type);
    }

    public Asset downloadNpc(String id, String rawUrl) throws Exception {
        byte[] bytes = download(rawUrl);
        return storeNpc(id, bytes);
    }

    public Asset downloadMedia(String id, String rawUrl) throws Exception {
        byte[] bytes = download(rawUrl);
        String type = detectType(bytes);
        return storeMedia(id, bytes, type);
    }

    public Asset storeNpc(String id, byte[] bytes) throws Exception {
        Asset asset = validate("npc:" + id, bytes, true, "png");
        Path folder = ensureNpcFolder(id);
        writeAtomically(folder.resolve("skin.png"), bytes);
        return asset;
    }

    public Asset storeMedia(String id, byte[] bytes) throws Exception {
        return storeMedia(id, bytes, detectType(bytes));
    }

    private Asset storeMedia(String id, byte[] bytes, String type) throws Exception {
        Asset asset = validate("media:" + id, bytes, false, type);
        Path folder = ensureMediaFolder(id);
        String fileName = "gif".equals(type) ? "media.gif" : "media.png";
        writeAtomically(folder.resolve(fileName), bytes);
        Files.deleteIfExists(folder.resolve("gif".equals(type) ? "media.png" : "media.gif"));
        return asset;
    }

    public void syncAll(ServerPlayerEntity player) {
        for (Asset asset : allAssets()) VisualAssetNetworking.send(player, asset);
    }

    public void broadcast(MinecraftServer server, Asset asset) {
        if (server == null || asset == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            VisualAssetNetworking.send(player, asset);
        }
    }

    public List<Asset> allAssets() {
        List<Asset> result = new ArrayList<>();
        scan(npcRoot, true, result);
        scan(mediaRoot, false, result);
        return result;
    }

    public Path npcFolder(String id) {
        return safeFolder(npcRoot, id);
    }

    public Path mediaFolder(String id) {
        return safeFolder(mediaRoot, id);
    }

    private void scan(Path base, boolean skin, List<Asset> result) {
        if (!Files.isDirectory(base)) return;
        try (var folders = Files.list(base)) {
            folders.filter(Files::isDirectory).forEach(folder -> {
                String id = folder.getFileName().toString();
                try {
                    result.add(skin ? loadNpc(id) : loadMedia(id));
                } catch (IllegalArgumentException ignored) {
                    // Empty folders are intentional until an administrator uploads the file.
                } catch (Exception exception) {
                    Emipokemon.LOGGER.warn("Ignored invalid visual asset folder {}: {}", folder, exception.getMessage());
                }
            });
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not scan visual asset directory {}", base, exception);
        }
    }

    private byte[] download(String rawUrl) throws Exception {
        URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("La URL debe usar HTTPS y tener un host válido.");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("La URL apunta a una red local o privada bloqueada.");
            }
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Emipokemon/" + Emipokemon.VERSION)
                .GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalArgumentException("La descarga respondió HTTP " + response.statusCode());
        }
        byte[] bytes;
        try (InputStream input = response.body()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo debe ocupar entre 1 byte y 4 MiB.");
        }
        return bytes;
    }

    private Asset validate(String key, byte[] bytes, boolean skin, String expectedType) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo debe ocupar entre 1 byte y 4 MiB.");
        }
        String type = detectType(bytes);
        if (!type.equals(expectedType)) throw new IllegalArgumentException("El contenido no coincide con el formato esperado.");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("El archivo no es una imagen válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (skin && (width != 64 || (height != 64 && height != 32))) {
                    throw new IllegalArgumentException("La skin debe medir 64x64 o 64x32 píxeles.");
                }
                if (!skin && (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION)) {
                    throw new IllegalArgumentException("La imagen debe medir como máximo 2048x2048 píxeles.");
                }
                if ("gif".equals(type) && reader.getNumImages(true) > MAX_GIF_FRAMES) {
                    throw new IllegalArgumentException("El GIF supera el límite de 160 fotogramas.");
                }
            } finally {
                reader.dispose();
            }
        }
        return new Asset(key, type, sha256(bytes), bytes);
    }

    private String detectType(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                && bytes[3] == 'G') return "png";
        if (bytes.length >= 6) {
            String signature = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(signature) || "GIF89a".equals(signature)) return "gif";
        }
        throw new IllegalArgumentException("Solo se admiten PNG y GIF reales.");
    }

    private Path safeFolder(Path base, String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("ID visual no válido.");
        Path target = base.resolve(normalized).normalize();
        if (!target.startsWith(base.normalize())) throw new IllegalArgumentException("Ruta visual no válida.");
        return target;
    }

    private void writeReadme(Path file, String content) throws Exception {
        if (!Files.exists(file)) Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private void requireSafeFileSize(Path file) throws Exception {
        long size = Files.size(file);
        if (size < 1L || size > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo debe ocupar entre 1 byte y 4 MiB.");
        }
    }

    private void writeAtomically(Path target, byte[] bytes) throws Exception {
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        Files.write(temporary, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    public record Asset(String key, String mediaType, String hash, byte[] bytes) {
    }
}
