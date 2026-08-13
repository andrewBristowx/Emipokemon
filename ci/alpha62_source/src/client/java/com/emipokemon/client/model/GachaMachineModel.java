package com.emipokemon.client.model;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public final class GachaMachineModel extends GeoModel<GachaMachineBlockEntity> {
    private static final Identifier STANDARD_MODEL = Identifier.of(Emipokemon.MOD_ID, "geo/standard_gacha_machine.geo.json");
    private static final Identifier EMI_MODEL = Identifier.of(Emipokemon.MOD_ID, "geo/emi_gacha_machine.geo.json");
    private static final Identifier STANDARD_TEXTURE = Identifier.of(Emipokemon.MOD_ID, "textures/block/standard_gacha_machine.png");
    private static final Identifier EMI_TEXTURE = Identifier.of(Emipokemon.MOD_ID, "textures/block/emi_gacha_machine.png");
    private static final Identifier ANIMATION = Identifier.of(Emipokemon.MOD_ID, "animations/standard_gacha_machine.animation.json");

    @Override
    public Identifier getModelResource(GachaMachineBlockEntity animatable) {
        return animatable.isEmiThemed() ? EMI_MODEL : STANDARD_MODEL;
    }

    @Override
    public Identifier getTextureResource(GachaMachineBlockEntity animatable) {
        return animatable.isEmiThemed() ? EMI_TEXTURE : STANDARD_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(GachaMachineBlockEntity animatable) {
        return ANIMATION;
    }
}
