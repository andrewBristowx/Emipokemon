package com.emipokemon.visual;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualRefreshRegressionTest {
    private static String source(String relative) throws Exception {
        return Files.readString(Path.of("src", relative));
    }

    @Test
    void bufferedArgbIsConvertedToNativeAbgr() throws Exception {
        String cache = source("client/java/com/emipokemon/client/visual/ClientVisualAssetCache.java");
        assertTrue(cache.contains("(a << 24) | (b << 16) | (g << 8) | r"));
        assertTrue(cache.contains("assembly.hash + \"/\" + index"));
        assertFalse(cache.contains("image.setColor(x, y, source.getRGB(x, y))"));
    }

    @Test
    void mediaUsesOpaqueCutoutAndFullBrightToResistShaders() throws Exception {
        String renderer = source("client/java/com/emipokemon/client/render/MediaDisplayRenderer.java");
        assertTrue(renderer.contains("getEntityCutoutNoCull(texture, false)"));
        assertFalse(renderer.contains("getEntityTranslucentEmissiveNoOutline"));
        assertTrue(renderer.contains("MAX_LIGHT_COORDINATE"));
    }

    @Test
    void hologramsRemainSafeWhenOptionalStreamotesBridgeIsUnavailable() throws Exception {
        String bridge = source("client/java/com/emipokemon/client/emote/StreamotesBridge.java");
        assertTrue(bridge.contains("Method method = getEmotes;"));
        assertTrue(bridge.contains("if (method == null) return List.of();"));
        assertTrue(bridge.contains("if (method == null) return null;"));
        assertTrue(bridge.contains("Method resolvedGetEmotes"));
        assertTrue(bridge.indexOf("getEmotes = resolvedGetEmotes;")
                > bridge.indexOf("Method resolvedGetLoader"));
    }
    @Test
    void hologramsUseVanillaTextDisplayInsteadOfCustomRendererForNewRuntime() throws Exception {
        String service = source("main/java/com/emipokemon/hologram/HologramService.java");
        String vanilla = source("main/java/com/emipokemon/hologram/VanillaTextHologram.java");
        String commands = source("main/java/com/emipokemon/hologram/HologramCommands.java");
        String registry = source("main/java/com/emipokemon/hologram/HologramRegistryStore.java");
        String core = source("main/java/com/emipokemon/Emipokemon.java");

        assertTrue(service.contains("EntityType.TEXT_DISPLAY"));
        assertTrue(service.contains("public static DisplayEntity.TextDisplayEntity move"));
        assertFalse(service.contains("ModRegistries.HOLOGRAM.create(world)"));
        assertTrue(vanilla.contains("EntityType.TEXT_DISPLAY.create(world)"));
        assertTrue(vanilla.contains("nbt.putString(\"billboard\", \"center\")"));
        assertTrue(vanilla.contains("nbt.putByte(\"see_through\", (byte) 0)"));
        assertTrue(vanilla.contains("Text.Serialization.toJsonString"));
        assertTrue(commands.contains("Hologramas persistidos"));
        assertTrue(commands.contains("text_display="));
        assertTrue(registry.contains("holograms-v1.properties"));
        assertTrue(registry.contains("StandardCopyOption.ATOMIC_MOVE"));
        assertTrue(core.contains("HologramService.restoreAll(server);"));
        assertTrue(service.contains("loaded.discard();"));
        String emotesButton = source("client/java/com/emipokemon/client/emote/EmotesButtonWidget.java");
        assertTrue(emotesButton.contains("public boolean keyPressed(int keyCode, int scanCode, int modifiers)"));
        assertTrue(emotesButton.contains("return false;"));
        assertFalse(emotesButton.contains("super.setFocused(false);"));
    }


    @Test
    void alpha60VersionIsConsistentInSource() throws Exception {
        String core = source("main/java/com/emipokemon/Emipokemon.java");
        assertTrue(core.contains("0.4.0-alpha.62"));
        assertFalse(core.contains("0.4.0-alpha.22"));
    }

    @Test
    void vanillaTextDisplayKeepsRendererWhileClientMixinAddsPlaceholdersOptionalEmotesAndDepthTesting() throws Exception {
        String mixin = source("client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java");
        String clientMixins = source("client/resources/emipokemon.client.mixins.json");
        String streamotes = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");
        String vanilla = source("main/java/com/emipokemon/hologram/VanillaTextHologram.java");
        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));
        assertTrue(mixin.contains("HologramStreamotesClientService.resolve(data.text())"));
        assertTrue(mixin.contains("data.flags() & ~0x02"));
        assertTrue(streamotes.contains("EmoticonRegistry"));
        assertTrue(streamotes.contains("makeEmoteStyle"));
        assertTrue(vanilla.contains("nbt.putByte(\"see_through\", (byte) 0)"));
    }

    @Test
    void alpha31ResolvesViewerTextBeforeSendingMetadata() throws Exception {
        String viewer = source("main/java/com/emipokemon/hologram/HologramViewerTextService.java");
        String commonMixins = source("main/resources/emipokemon.mixins.json");
        String clientMixins = source("client/resources/emipokemon.client.mixins.json");
        assertTrue(viewer.contains("EntityTrackerUpdateS2CPacket"));
        assertTrue(viewer.contains("DataTracker.SerializedEntry.of"));
        assertTrue(viewer.contains("{player}"));
        assertTrue(viewer.contains("return Text.literal(resolved).setStyle(base);"));
        assertTrue(commonMixins.contains("TextDisplayTrackedDataAccessor"));
        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));
    }

    @Test
    void alpha32ResolvesStreamotesFromClientRegistry() throws Exception {
        String client = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");
        assertTrue(client.contains("EmoticonRegistry"));
        assertTrue(client.contains("Compat"));
        assertTrue(client.contains("makeEmoteStyle"));
        assertTrue(client.contains("client.world.getEntities()"));
        assertTrue(client.contains("display.getDataTracker().set(trackedText, resolved, true)"));
        assertTrue(client.contains("return changed ? out : original"));
    }

    @Test
    void alpha33EmotesButtonDoesNotStealChatFocus() throws Exception {
        String button = source("client/java/com/emipokemon/client/emote/EmotesButtonWidget.java");
        String controller = source("client/java/com/emipokemon/client/emote/ChatEmoteController.java");
        assertTrue(button.contains("public boolean keyPressed(int keyCode, int scanCode, int modifiers)"));
        assertTrue(button.contains("return false;"));
        assertFalse(button.contains("public void setFocused(boolean focused)"));
        assertFalse(button.contains("super.setFocused(false)"));
        assertTrue(controller.contains("chatScreen.setFocused(field);"));
        assertTrue(controller.contains("field.setFocused(true);"));
        assertTrue(controller.contains("activeOverlay.focusChatOnNextTick();"));
    }

    @Test
    void alpha34ResolvesEmotesBeforeFirstRenderedFrame() throws Exception {
        String mixin = source("client/java/com/emipokemon/client/mixin/TextDisplayHologramMixin.java");
        String clientMixins = source("client/resources/emipokemon.client.mixins.json");
        String streamotes = source("client/java/com/emipokemon/client/emote/HologramStreamotesClientService.java");
        assertTrue(clientMixins.contains("TextDisplayHologramMixin"));
        assertTrue(mixin.contains("HologramStreamotesClientService.resolve(data.text())"));
        assertTrue(mixin.contains("data.flags() & ~0x02"));
        assertTrue(streamotes.contains("public static Text resolve(Text original)"));
        assertTrue(streamotes.contains("END_CLIENT_TICK"));
    }

    @Test
    void alpha35DoesNotPeriodicallyResendUnchangedHologramMetadata() throws Exception {
        String viewer = source("main/java/com/emipokemon/hologram/HologramViewerTextService.java");
        assertTrue(viewer.contains("EntityTrackingEvents.START_TRACKING"));
        assertTrue(viewer.contains("EntityTrackingEvents.STOP_TRACKING"));
        assertTrue(viewer.contains("previous.fingerprint().equals(fingerprint)"));
        assertFalse(viewer.contains("ticks - previous.tick() < 100L"));
    }

}

