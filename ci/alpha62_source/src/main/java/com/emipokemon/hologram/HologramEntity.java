package com.emipokemon.hologram;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Locale;

public final class HologramEntity extends Entity {
    private static final TrackedData<String> HOLOGRAM_ID = DataTracker.registerData(
            HologramEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Text> TEXT = DataTracker.registerData(
            HologramEntity.class, TrackedDataHandlerRegistry.TEXT_COMPONENT);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(
            HologramEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(
            HologramEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public HologramEntity(EntityType<? extends HologramEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setNoGravity(true);
        // The renderer lifts the text 2.35 blocks above this tiny anchor entity.
        // Never let vanilla frustum culling discard the anchor before the label is drawn.
        ignoreCameraFrustum = true;
    }


    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, entityTrackerEntry);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(HOLOGRAM_ID, "");
        builder.add(TEXT, Text.literal("Holograma"));
        builder.add(SCALE, 1.0F);
        builder.add(COLOR, 0xFFFFFFFF);
    }

    public String hologramId() {
        return dataTracker.get(HOLOGRAM_ID);
    }

    public void setHologramId(String value) {
        String id = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        dataTracker.set(HOLOGRAM_ID, id.length() > 32 ? id.substring(0, 32) : id);
    }

    public Text hologramText() {
        return dataTracker.get(TEXT);
    }

    public void setHologramText(String value) {
        String text = value == null ? "" : value;
        dataTracker.set(TEXT, Text.literal(text.length() > 512 ? text.substring(0, 512) : text));
    }

    public float hologramScale() {
        return dataTracker.get(SCALE);
    }

    public void setHologramScale(float value) {
        dataTracker.set(SCALE, Math.clamp(value, 0.25F, 8.0F));
    }

    public int hologramColor() {
        return dataTracker.get(COLOR);
    }

    public void setHologramColor(int value) {
        dataTracker.set(COLOR, value | 0xFF000000);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setHologramId(nbt.getString("EmipokemonHologramId"));
        setHologramText(nbt.getString("EmipokemonHologramText"));
        float scale = nbt.getFloat("EmipokemonHologramScale");
        setHologramScale(scale <= 0.0F ? 1.0F : scale);
        setHologramColor(nbt.contains("EmipokemonHologramColor")
                ? nbt.getInt("EmipokemonHologramColor") : 0xFFFFFFFF);
        setInvulnerable(true);
        setNoGravity(true);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("EmipokemonHologramId", hologramId());
        nbt.putString("EmipokemonHologramText", hologramText().getString());
        nbt.putFloat("EmipokemonHologramScale", hologramScale());
        nbt.putInt("EmipokemonHologramColor", hologramColor());
    }

    @Override
    public void tick() {
        super.tick();
        HologramService.reconcile(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 4096.0D;
    }
}
