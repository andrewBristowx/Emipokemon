package com.emipokemon.client.render;

import com.emipokemon.Emipokemon;
import com.emipokemon.client.visual.ClientVisualAssetCache;
import com.emipokemon.visual.MediaDisplayEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class MediaDisplayRenderer extends EntityRenderer<MediaDisplayEntity> {
    private static final Identifier FALLBACK = Identifier.of(Emipokemon.MOD_ID, "textures/block/standard_gacha_machine.png");

    public MediaDisplayRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(MediaDisplayEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertices, int light) {
        Identifier texture = getTexture(entity);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - entity.getYaw()));
        matrices.translate(0.0D, entity.displayHeight() * 0.5D, 0.0D);
        matrices.scale(entity.displayWidth(), entity.displayHeight(), 1.0f);
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();
        // Cutout keeps visible pixels opaque under Iris/shader packs while preserving transparent backgrounds.
        // Full-bright light below prevents day/night from recoloring the uploaded artwork.
        VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(texture, false));
        vertex(consumer, matrix, entry, -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, light);
        vertex(consumer, matrix, entry, 0.5f, -0.5f, 0.0f, 1.0f, 1.0f, light);
        vertex(consumer, matrix, entry, 0.5f, 0.5f, 0.0f, 1.0f, 0.0f, light);
        vertex(consumer, matrix, entry, -0.5f, 0.5f, 0.0f, 0.0f, 0.0f, light);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, MatrixStack.Entry entry,
                        float x, float y, float z, float u, float v, int light) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public Identifier getTexture(MediaDisplayEntity entity) {
        return ClientVisualAssetCache.texture("media:" + entity.displayId(), FALLBACK);
    }
}
