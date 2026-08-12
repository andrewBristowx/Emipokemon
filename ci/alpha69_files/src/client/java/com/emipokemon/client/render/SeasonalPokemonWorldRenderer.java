package com.emipokemon.client.render;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.WeakHashMap;

/** Stable world-space featured Pokémon plus independently billboarded labels. */
final class SeasonalPokemonWorldRenderer {
    private static final Map<GachaMachineBlockEntity, CachedPokemon> CACHE = new WeakHashMap<>();
    private static boolean renderFailureLogged;

    private SeasonalPokemonWorldRenderer() { }

    static void draw(GachaMachineBlockEntity machine, float partialTick, MatrixStack matrices,
                     VertexConsumerProvider vertices, TextRenderer textRenderer) {
        String species = normalizeSpecies(machine.getFeaturedSpeciesId());
        if (species.isBlank() || machine.getWorld() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        float bob = (float) Math.sin(machine.getWorld().getTime() * 0.075D
                + machine.getPos().asLong() * 0.01D) * 0.055F;

        renderWorldPokemon(machine, species, partialTick, matrices, vertices, client, bob);

        // Never share the quaternion with the Pokémon renderer: Cobblemon profile rendering can mutate it.
        Quaternionf cleanCameraRotation = new Quaternionf(client.getEntityRenderDispatcher().getRotation());
        drawTextLine(matrices, vertices, textRenderer, cleanCameraRotation, 2.34D,
                "POKÉMON DE TEMPORADA", machine.isEmiThemed() ? 0xFFFFA6E4 : 0xFF8DEBFF, -6.0F);
        drawTextLine(matrices, vertices, textRenderer, cleanCameraRotation, 2.34D,
                machine.getFeaturedPokemonName(), 0xFFFFFFFF, 6.0F);
    }

    private static void renderWorldPokemon(GachaMachineBlockEntity machine, String species, float partialTick,
                                           MatrixStack matrices, VertexConsumerProvider vertices,
                                           MinecraftClient client, float bob) {
        try {
            CachedPokemon cached = CACHE.get(machine);
            if (cached == null || !cached.speciesId.equals(species) || cached.entity.isRemoved()) {
                PokemonEntity entity = CobblemonEntities.POKEMON.create(machine.getWorld());
                if (entity == null) return;
                NbtCompound nbt = new NbtCompound();
                nbt.putString("species", "cobblemon:" + species);
                entity.getPokemon().loadFromNBT(nbt);
                cached = new CachedPokemon(species, entity);
                CACHE.put(machine, cached);
            }

            PokemonEntity entity = cached.entity;
            float width = Math.max(0.25F, entity.getWidth());
            float height = Math.max(0.25F, entity.getHeight());
            float fit = Math.min(0.82F, Math.min(1.45F / width, 1.35F / height));
            fit = Math.max(0.28F, fit);

            matrices.push();
            matrices.translate(0.5D, 2.62D + bob, 0.5D);
            // Keep the display upright, but turn it toward the player around Y only.
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - client.gameRenderer.getCamera().getYaw()));
            matrices.scale(fit, fit, fit);
            client.getEntityRenderDispatcher().render(entity, 0.0D, 0.0D, 0.0D,
                    0.0F, partialTick, matrices, vertices, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            matrices.pop();
        } catch (Throwable exception) {
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                Emipokemon.LOGGER.warn("Could not render the featured Cobblemon above a gacha machine", exception);
            }
        }
    }

    private static void drawTextLine(MatrixStack matrices, VertexConsumerProvider vertices, TextRenderer renderer,
                                     Quaternionf cameraRotation, double y, String value, int color, float lineY) {
        if (value == null || value.isBlank()) return;
        matrices.push();
        matrices.translate(0.5D, y, 0.5D);
        matrices.multiply(new Quaternionf(cameraRotation));
        matrices.scale(-0.018F, -0.018F, 0.018F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Text text = Text.literal(value);
        float x = -renderer.getWidth(text) / 2.0F;
        renderer.draw(text, x, lineY, color, false, matrix, vertices,
                TextRenderer.TextLayerType.SEE_THROUGH, 0xB0100818,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        renderer.draw(text, x, lineY, color, false, matrix, vertices,
                TextRenderer.TextLayerType.NORMAL, 0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }

    private static String normalizeSpecies(String species) {
        if (species == null) return "";
        String normalized = species.strip().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("cobblemon:") ? normalized.substring("cobblemon:".length()) : normalized;
    }

    private record CachedPokemon(String speciesId, PokemonEntity entity) { }
}
