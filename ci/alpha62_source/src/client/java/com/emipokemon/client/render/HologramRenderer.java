package com.emipokemon.client.render;

import com.emipokemon.client.emote.HologramTextResolver;
import com.emipokemon.hologram.HologramEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public final class HologramRenderer extends EntityRenderer<HologramEntity> {
    private final TextRenderer textRenderer;

    public HologramRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.textRenderer = context.getTextRenderer();
    }

    @Override
    public void render(HologramEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertices, int light) {
        matrices.push();
        // The entity position is the floor anchor selected by the administrator.
        // Lift the label above the player's head instead of drawing it inside the block at their feet.
        matrices.translate(0.0D, 2.35D, 0.0D);
        matrices.multiply(dispatcher.getRotation());
        float scale = 0.025F * entity.hologramScale();
        matrices.scale(-scale, -scale, scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Text text = HologramTextResolver.resolve(entity);
        float x = -textRenderer.getWidth(text) / 2.0F;

        // SEE_THROUGH uses the shader-safe text layer. The four black passes form an opaque
        // outline, and the final normal pass keeps vanilla depth behaviour when shaders allow it.
        int outline = 0xFF130A16;
        for (int[] offset : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            textRenderer.draw(text, x + offset[0], offset[1], outline, false,
                    matrix, vertices, TextRenderer.TextLayerType.SEE_THROUGH, 0,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        textRenderer.draw(text, x, 0.0F, entity.hologramColor(), false,
                matrix, vertices, TextRenderer.TextLayerType.SEE_THROUGH, 0xB0000000,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        textRenderer.draw(text, x, 0.0F, entity.hologramColor(), false,
                matrix, vertices, TextRenderer.TextLayerType.NORMAL, 0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    @Override
    public Identifier getTexture(HologramEntity entity) {
        return Identifier.ofVanilla("textures/misc/white.png");
    }
}
