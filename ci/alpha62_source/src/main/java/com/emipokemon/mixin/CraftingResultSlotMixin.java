package com.emipokemon.mixin;

import com.emipokemon.Emipokemon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {
    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void emipokemon$recordCraft(PlayerEntity player, ItemStack stack, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayerEntity serverPlayer && !stack.isEmpty()) {
            Emipokemon.progressionService().onCrafted(serverPlayer, stack.copy());
        }
    }
}
