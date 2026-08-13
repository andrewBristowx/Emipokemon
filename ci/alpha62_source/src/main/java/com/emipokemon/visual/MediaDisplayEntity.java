package com.emipokemon.visual;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.Locale;

public final class MediaDisplayEntity extends Entity {
    private static final TrackedData<String> DISPLAY_ID = DataTracker.registerData(
            MediaDisplayEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Float> DISPLAY_WIDTH = DataTracker.registerData(
            MediaDisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> DISPLAY_HEIGHT = DataTracker.registerData(
            MediaDisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public MediaDisplayEntity(EntityType<? extends MediaDisplayEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DISPLAY_ID, "");
        builder.add(DISPLAY_WIDTH, 2.0f);
        builder.add(DISPLAY_HEIGHT, 1.0f);
    }

    public String displayId() {
        return dataTracker.get(DISPLAY_ID);
    }

    public void setDisplayId(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        dataTracker.set(DISPLAY_ID, normalized.length() > 32 ? normalized.substring(0, 32) : normalized);
    }

    public float displayWidth() {
        return dataTracker.get(DISPLAY_WIDTH);
    }

    public float displayHeight() {
        return dataTracker.get(DISPLAY_HEIGHT);
    }

    public void setDisplaySize(float width, float height) {
        dataTracker.set(DISPLAY_WIDTH, Math.clamp(width, 0.25f, 16.0f));
        dataTracker.set(DISPLAY_HEIGHT, Math.clamp(height, 0.25f, 16.0f));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setDisplayId(nbt.getString("EmipokemonMediaId"));
        setDisplaySize(nbt.getFloat("EmipokemonMediaWidth"), nbt.getFloat("EmipokemonMediaHeight"));
        setInvulnerable(true);
        setNoGravity(true);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("EmipokemonMediaId", displayId());
        nbt.putFloat("EmipokemonMediaWidth", displayWidth());
        nbt.putFloat("EmipokemonMediaHeight", displayHeight());
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
