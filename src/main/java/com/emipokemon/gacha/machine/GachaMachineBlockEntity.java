package com.emipokemon.gacha.machine;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.GachaRollResult;
import com.emipokemon.gacha.GachaService;
import com.emipokemon.gacha.GachaTier;
import com.emipokemon.gacha.banner.BannerDefinition;
import com.emipokemon.registry.ModRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

import java.util.Locale;
import java.util.UUID;

public final class GachaMachineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gacha_machine.idle");
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("animation.gacha_machine.activate");
    private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("animation.gacha_machine.roll");
    private static final RawAnimation RESULT_COMMON = RawAnimation.begin().thenPlay("animation.gacha_machine.result_common");
    private static final RawAnimation RESULT_EPIC = RawAnimation.begin().thenPlay("animation.gacha_machine.result_epic");
    private static final RawAnimation RESULT_LEGENDARY = RawAnimation.begin().thenPlay("animation.gacha_machine.result_legendary");
    private static final RawAnimation ERROR = RawAnimation.begin().thenPlay("animation.gacha_machine.error");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private String bannerId = "rayquaza_hoenn";
    private GachaMachineState machineState = GachaMachineState.IDLE;
    private GachaTier resultTier;
    private UUID activePlayerUuid;
    private UUID activeTransactionId;
    private int phaseTicks;
    private transient GachaService.PreparedPull preparedPull;

    public GachaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.GACHA_MACHINE_BLOCK_ENTITY, pos, state);
    }

    public String getBannerId() {
        return bannerId;
    }

    public void setBannerId(String bannerId) {
        if (bannerId == null || bannerId.isBlank()) return;
        this.bannerId = bannerId.toLowerCase(Locale.ROOT);
        markAndSync();
    }

    public GachaMachineState getMachineState() {
        return machineState;
    }

    public GachaTier getResultTier() {
        return resultTier;
    }

    public boolean isBusy() {
        return machineState.isBusy();
    }

    public GachaService.PrepareOutcome tryStartPull(ServerPlayerEntity player) {
        if (world == null || world.isClient) {
            return GachaService.PrepareOutcome.failure("La maquina solo puede activarse desde el servidor.");
        }
        if (isBusy()) {
            return GachaService.PrepareOutcome.failure("La maquina esta procesando otra tirada.");
        }

        BannerDefinition banner = Emipokemon.bannerManager().get(bannerId);
        if (banner == null || !banner.enabled) {
            setError();
            return GachaService.PrepareOutcome.failure("Banner no encontrado o desactivado: " + bannerId);
        }

        Item requiredTicket = isEmiBanner(bannerId)
                ? ModRegistries.EMI_SPECIAL_BANNER_TICKET
                : ModRegistries.GACHA_TICKET;
        if (player.getInventory().count(requiredTicket) < 1) {
            return GachaService.PrepareOutcome.failure(
                    isEmiBanner(bannerId)
                            ? "Necesitas 1x Ticket Especial de Emi."
                            : "Necesitas 1x Gacha Ticket."
            );
        }

        GachaService.PrepareOutcome preparation = Emipokemon.gachaService().preparePull(player, bannerId);
        if (!preparation.success()) {
            setError();
            return preparation;
        }

        this.preparedPull = preparation.preparedPull();
        this.activePlayerUuid = player.getUuid();
        this.activeTransactionId = preparedPull.transactionId();
        this.resultTier = preparedPull.result().tier();
        this.machineState = GachaMachineState.ACTIVATING;
        this.phaseTicks = 10;
        markAndSync();
        return preparation;
    }

    public void forceReset() {
        cancelPreparedReservation();
        clearToIdle();
        markAndSync();
    }

    private void setError() {
        cancelPreparedReservation();
        this.machineState = GachaMachineState.ERROR;
        this.resultTier = null;
        this.activePlayerUuid = null;
        this.activeTransactionId = null;
        this.preparedPull = null;
        this.phaseTicks = 24;
        markAndSync();
    }

    private void clearToIdle() {
        this.machineState = GachaMachineState.IDLE;
        this.resultTier = null;
        this.activePlayerUuid = null;
        this.activeTransactionId = null;
        this.preparedPull = null;
        this.phaseTicks = 0;
    }

    private void cancelPreparedReservation() {
        if (activePlayerUuid != null && activeTransactionId != null) {
            Emipokemon.gachaService().cancelPreparedPull(activePlayerUuid, activeTransactionId);
        }
    }

    private BannerDefinition.Currency ticketCost() {
        BannerDefinition.Currency cost = new BannerDefinition.Currency();
        cost.type = "ITEM";
        cost.itemId = isEmiBanner(bannerId)
                ? "emipokemon:emi_special_banner_ticket"
                : "emipokemon:gacha_ticket";
        cost.amount = 1;
        cost.normalize();
        return cost;
    }

    private boolean isEmiBanner(String id) {
        String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return normalized.startsWith("emi_") || normalized.contains("emi_special");
    }

    private GachaService.PullOutcome commitPrepared(ServerWorld serverWorld) {
        if (preparedPull == null || activePlayerUuid == null || activeTransactionId == null) {
            return GachaService.PullOutcome.failure("La tirada preparada ya no esta disponible.");
        }

        ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(activePlayerUuid);
        if (player == null) {
            cancelPreparedReservation();
            return GachaService.PullOutcome.failure("El jugador se desconecto antes de completar la tirada.");
        }

        GachaService.PullOutcome outcome = Emipokemon.gachaService()
                .commitPreparedPull(player, preparedPull, ticketCost());
        if (outcome.success()) {
            GachaRollResult result = outcome.result();
            player.sendMessage(Text.literal(
                    "GACHA: " + result.tier().name() + " -> " + result.pokemon().displayName()
                            + " Nv." + result.level()
                            + (result.shiny() ? " SHINY" : "")
            ), false);
        } else {
            player.sendMessage(Text.literal("Gacha: " + outcome.error()), false);
        }
        return outcome;
    }

    public static void tick(World world, BlockPos pos, BlockState state, GachaMachineBlockEntity machine) {
        if (world.isClient || machine.phaseTicks <= 0) return;

        machine.phaseTicks--;
        if (machine.phaseTicks > 0) return;

        switch (machine.machineState) {
            case ACTIVATING -> {
                machine.machineState = GachaMachineState.ROLLING;
                machine.phaseTicks = 60;
            }
            case ROLLING -> {
                if (!(world instanceof ServerWorld serverWorld)) {
                    machine.setError();
                    return;
                }

                GachaService.PullOutcome committed = machine.commitPrepared(serverWorld);
                if (!committed.success()) {
                    machine.setError();
                    return;
                }

                machine.preparedPull = null;
                machine.activeTransactionId = null;
                machine.resultTier = committed.result().tier();

                if (machine.resultTier.isAtLeast(GachaTier.LEGENDARY)) {
                    machine.machineState = GachaMachineState.REVEAL_LEGENDARY;
                    machine.phaseTicks = 42;
                } else if (machine.resultTier.isAtLeast(GachaTier.EPIC)) {
                    machine.machineState = GachaMachineState.REVEAL_EPIC;
                    machine.phaseTicks = 30;
                } else {
                    machine.machineState = GachaMachineState.REVEAL_COMMON;
                    machine.phaseTicks = 20;
                }
            }
            case REVEAL_COMMON, REVEAL_EPIC, REVEAL_LEGENDARY, ERROR -> machine.clearToIdle();
            case IDLE -> machine.phaseTicks = 0;
        }
        machine.markAndSync();
    }

    private void markAndSync() {
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    public void markRemoved() {
        cancelPreparedReservation();
        super.markRemoved();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("BannerId", bannerId);
        nbt.putString("MachineState", machineState.name());
        nbt.putInt("PhaseTicks", phaseTicks);
        if (resultTier != null) nbt.putString("ResultTier", resultTier.name());
        if (activePlayerUuid != null) nbt.putUuid("ActivePlayer", activePlayerUuid);
        if (activeTransactionId != null) nbt.putUuid("ActiveTransaction", activeTransactionId);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("BannerId")) bannerId = nbt.getString("BannerId");
        try {
            machineState = GachaMachineState.valueOf(nbt.getString("MachineState"));
        } catch (Exception ignored) {
            machineState = GachaMachineState.IDLE;
        }
        phaseTicks = Math.max(0, nbt.getInt("PhaseTicks"));
        resultTier = nbt.contains("ResultTier") ? GachaTier.parse(nbt.getString("ResultTier"), null) : null;
        activePlayerUuid = nbt.containsUuid("ActivePlayer") ? nbt.getUuid("ActivePlayer") : null;
        activeTransactionId = nbt.containsUuid("ActiveTransaction") ? nbt.getUuid("ActiveTransaction") : null;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "machine", 2, this::animationController));
    }

    private PlayState animationController(AnimationState<GachaMachineBlockEntity> animationState) {
        RawAnimation animation = switch (machineState) {
            case IDLE -> IDLE;
            case ACTIVATING -> ACTIVATE;
            case ROLLING -> ROLL;
            case REVEAL_COMMON -> RESULT_COMMON;
            case REVEAL_EPIC -> RESULT_EPIC;
            case REVEAL_LEGENDARY -> RESULT_LEGENDARY;
            case ERROR -> ERROR;
        };
        return animationState.setAndContinue(animation);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
