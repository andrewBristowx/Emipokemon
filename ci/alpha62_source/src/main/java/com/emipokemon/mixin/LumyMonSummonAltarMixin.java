package com.emipokemon.mixin;

import com.emipokemon.integration.CobbleverseMissionHooks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.lumyverse.lumymon.block.custom.SummonAltar", remap = false)
abstract class LumyMonSummonAltarMixin {
    @Inject(
            method = {"method_55765", "onUseWithItem"},
            at = @At("RETURN"),
            remap = false
    )
    private void emipokemon$acceptedInvocation(ItemStack stack, BlockState state, World world,
                                               BlockPos pos, PlayerEntity player, Hand hand,
                                               BlockHitResult hit,
                                               CallbackInfoReturnable<ItemActionResult> callback) {
        if (!world.isClient
                && callback.getReturnValue() == ItemActionResult.CONSUME
                && player instanceof ServerPlayerEntity serverPlayer) {
            CobbleverseMissionHooks.altarInvocationAccepted(
                    serverPlayer, Registries.BLOCK.getId(state.getBlock()));
        }
    }
}
