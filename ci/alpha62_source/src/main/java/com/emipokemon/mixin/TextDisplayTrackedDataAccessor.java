package com.emipokemon.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes vanilla TextDisplayEntity's tracked text slot for per-viewer metadata packets. */
@Mixin(DisplayEntity.TextDisplayEntity.class)
public interface TextDisplayTrackedDataAccessor {
    @Accessor("TEXT")
    static TrackedData<Text> emipokemon$getTextTrackedData() {
        throw new AssertionError();
    }
}
