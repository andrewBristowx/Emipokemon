package com.emipokemon.client.mixin;

import com.emipokemon.client.emote.HologramStreamotesClientService;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Minecraft's proven text_display renderer and replaces only the Text payload for
 * Emipokemon-tagged displays. This makes placeholders viewer-local and Streamotes optional.
 */
@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class TextDisplayHologramMixin {
    @Inject(
            method = "getData(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;)Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$Data;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void emipokemon$resolveHologramText(
            DisplayEntity.TextDisplayEntity entity,
            CallbackInfoReturnable<DisplayEntity.TextDisplayEntity.Data> cir
    ) {
        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();
        if (data == null) return;

        // Resolve Streamotes during getData(), before vanilla can draw a raw :emote: token.
        // The END_CLIENT_TICK service remains the stable-state updater; this render-time path
        // specifically removes the one-frame flash caused by periodic server metadata refreshes.
        Text resolved = HologramStreamotesClientService.resolve(data.text());
        byte depthTestedFlags = (byte) (data.flags() & ~0x02);
        boolean textChanged = resolved != data.text() && !resolved.equals(data.text());
        boolean flagsChanged = depthTestedFlags != data.flags();
        if (!textChanged && !flagsChanged) return;
        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(
                resolved, data.lineWidth(), data.textOpacity(), data.backgroundColor(), depthTestedFlags));
    }
}
