package com.emipokemon.mixin;

import com.emipokemon.Emipokemon;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void emipokemon$recordPlacedBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> callback) {
        if (callback.getReturnValue().isAccepted() && context.getPlayer() instanceof ServerPlayerEntity player) {
            Emipokemon.progressionService().onBlockPlaced(player);
        }
    }
}
