package com.emipokemon.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Adapts Cobblemon 1.7.3's 13-argument portrait API and its older 16-argument variant. */
public final class PokemonPortraitRenderer {
    private static volatile Method drawMethod;
    private static volatile Constructor<?> stateConstructor;
    private static volatile Object profilePose;
    private static volatile boolean lookupFailed;

    private PokemonPortraitRenderer() { }

    public static boolean draw(DrawContext context, String speciesId, int centerX, int bottomY, int scale) {
        boolean pushed = false;
        try {
            Method draw = resolveMethod();
            if (draw == null) return false;
            Identifier species = Identifier.tryParse(speciesId);
            if (species == null) return false;
            Object poseState = stateConstructor.newInstance(new Object[] {null});
            context.getMatrices().push();
            pushed = true;
            context.getMatrices().translate(centerX, bottomY, 170.0F);
            if (draw.getParameterCount() == 13) {
                draw.invoke(null, species, context.getMatrices(), new Quaternionf(), profilePose, poseState,
                        0.0F, (float) scale, true, true, 0.0F, 0.0F, 1.0F, 1.0F);
            } else {
                draw.invoke(null, species, context.getMatrices(), new Quaternionf(), profilePose, poseState,
                        0.0F, (float) scale, true, true, false, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            context.getMatrices().pop();
            return true;
        } catch (Throwable exception) {
            if (pushed) try { context.getMatrices().pop(); } catch (Throwable ignored) { }
            return false;
        }
    }

    private static Method resolveMethod() throws Exception {
        if (drawMethod != null || lookupFailed) return drawMethod;
        synchronized (PokemonPortraitRenderer.class) {
            if (drawMethod != null || lookupFailed) return drawMethod;
            Class<?> stateClass = Class.forName("com.cobblemon.mod.common.client.render.models.blockbench.PosableState");
            for (Constructor<?> constructor : stateClass.getConstructors())
                if (constructor.getParameterCount() == 1) { stateConstructor = constructor; break; }
            Class<?> poseTypeClass = Class.forName("com.cobblemon.mod.common.entity.PoseType");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object profile = Enum.valueOf((Class) poseTypeClass.asSubclass(Enum.class), "PROFILE");
            Method fallback = null;
            Class<?> utilities = Class.forName("com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt");
            for (Method method : utilities.getMethods()) {
                if (!method.getName().equals("drawProfilePokemon")) continue;
                if (method.getParameterCount() == 13) { drawMethod = method; break; }
                if (method.getParameterCount() == 16) fallback = method;
            }
            if (drawMethod == null) drawMethod = fallback;
            profilePose = profile;
            if (drawMethod == null || stateConstructor == null) lookupFailed = true;
            return drawMethod;
        }
    }
}
