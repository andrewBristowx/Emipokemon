package com.emipokemon.casino;

import com.emipokemon.registry.ModRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CasinoMachineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.casino.idle");
    private static final RawAnimation PLAY = RawAnimation.begin().thenPlay("animation.casino.play");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int animationTicks;
    private boolean lastPlaying;

    public CasinoMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.CASINO_MACHINE_BLOCK_ENTITY, pos, state);
    }

    public CasinoGameType gameType() {
        return ModRegistries.casinoType(getCachedState().getBlock());
    }

    public void activate() {
        animationTicks = 36;
        markAndSync();
    }

    public boolean playing() {
        return animationTicks > 0;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CasinoMachineBlockEntity machine) {
        if (world.isClient || machine.animationTicks <= 0) return;
        if (--machine.animationTicks == 0) machine.markAndSync();
    }

    private void markAndSync() {
        markDirty();
        if (world instanceof ServerWorld serverWorld) serverWorld.getChunkManager().markForUpdate(pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("CasinoAnimationTicks", animationTicks);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        animationTicks = Math.max(0, nbt.getInt("CasinoAnimationTicks"));
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "casino", 1, this::controller));
    }

    private PlayState controller(AnimationState<CasinoMachineBlockEntity> state) {
        if (lastPlaying != playing()) {
            state.getController().forceAnimationReset();
            lastPlaying = playing();
        }
        return state.setAndContinue(playing() ? PLAY : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
