package com.emipokemon.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualAssetSafetyTest {
    @TempDir
    Path temp;

    @Test
    void keepsServerSideUrlAndFileLimitsExplicit() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/emipokemon/visual/VisualAssetService.java"));
        assertTrue(source.contains("\"https\".equalsIgnoreCase(uri.getScheme())"));
        assertTrue(source.contains("isLoopbackAddress"));
        assertTrue(source.contains("isSiteLocalAddress"));
        assertTrue(source.contains("MAX_BYTES = 4 * 1024 * 1024"));
        assertTrue(source.contains("MAX_DIMENSION = 2048"));
        assertTrue(source.contains("MAX_GIF_FRAMES = 160"));
        assertTrue(source.contains("target.startsWith(base.normalize())"));
    }

    @Test
    void transfersAssetsInBoundedChunks() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/emipokemon/visual/VisualAssetNetworking.java"));
        assertTrue(source.contains("CHUNK_BYTES = 18_000"));
        assertTrue(source.contains("AssetChunkPayload"));
    }

    @Test
    void createsOwnedFoldersAndValidatesUploadedFiles() throws Exception {
        VisualAssetService service = new VisualAssetService(temp.resolve("emipokemon"));
        service.initialize();

        Path npcFolder = service.ensureNpcFolder("profesora_emi");
        assertTrue(Files.isRegularFile(npcFolder.resolve("LEEME.txt")));
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png",
                npcFolder.resolve("skin.png").toFile());
        assertEquals("npc:profesora_emi", service.loadNpc("profesora_emi").key());

        Path invalidFolder = service.ensureNpcFolder("skin_invalida");
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), "png",
                invalidFolder.resolve("skin.png").toFile());
        assertThrows(IllegalArgumentException.class, () -> service.loadNpc("skin_invalida"));

        Path mediaFolder = service.ensureMediaFolder("bienvenida");
        ImageIO.write(new BufferedImage(320, 180, BufferedImage.TYPE_INT_ARGB), "png",
                mediaFolder.resolve("media.png").toFile());
        assertEquals("media:bienvenida", service.loadMedia("bienvenida").key());

        Path oversizedFolder = service.ensureMediaFolder("demasiado_grande");
        Files.write(oversizedFolder.resolve("media.png"), new byte[VisualAssetService.MAX_BYTES + 1]);
        assertThrows(IllegalArgumentException.class, () -> service.loadMedia("demasiado_grande"));
        assertThrows(IllegalArgumentException.class, () -> service.ensureMediaFolder("../fuera"));
    }
}
