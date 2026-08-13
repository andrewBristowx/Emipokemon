package com.emipokemon.client.model;

import com.emipokemon.Emipokemon;
import com.emipokemon.casino.CasinoMachineBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class CasinoMachineModel extends GeoModel<CasinoMachineBlockEntity> {
    @Override
    public Identifier getModelResource(CasinoMachineBlockEntity animatable) {
        return Identifier.of(Emipokemon.MOD_ID, "geo/casino_" + animatable.gameType().id() + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(CasinoMachineBlockEntity animatable) {
        return Identifier.of(Emipokemon.MOD_ID, "textures/block/casino_" + animatable.gameType().id() + ".png");
    }

    @Override
    public Identifier getAnimationResource(CasinoMachineBlockEntity animatable) {
        return Identifier.of(Emipokemon.MOD_ID, "animations/casino.animation.json");
    }
}
