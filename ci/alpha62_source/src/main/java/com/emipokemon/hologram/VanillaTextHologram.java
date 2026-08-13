package com.emipokemon.hologram;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Runtime representation for Emipokemon holograms using Minecraft's vanilla text_display entity.
 * The persistent source of truth remains {@link HologramRegistryStore}; this class only builds the
 * visual entity that vanilla Minecraft already knows how to spawn, synchronize and render.
 */
final class VanillaTextHologram {
    static final double VERTICAL_OFFSET = 2.35D;
    static final String TAG_PREFIX = "emipokemon:hologram:";
    static final String TEXT_MARKER_PREFIX = "emipokemon:hologram-text:";

    private VanillaTextHologram() {}

    static DisplayEntity.TextDisplayEntity spawn(ServerWorld world, HologramRegistryStore.Entry entry) {
        if (world == null || entry == null) return null;
        DisplayEntity.TextDisplayEntity display = EntityType.TEXT_DISPLAY.create(world);
        if (display == null) return null;
        configure(display, world, entry);
        return world.spawnEntity(display) ? display : null;
    }

    static void configure(DisplayEntity.TextDisplayEntity display, ServerWorld world,
                          HologramRegistryStore.Entry entry) {
        display.refreshPositionAndAngles(entry.x(), entry.y() + VERTICAL_OFFSET, entry.z(), 0.0F, 0.0F);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.ignoreCameraFrustum = true;

        NbtCompound nbt = new NbtCompound();
        display.writeNbt(nbt);

        // Empty root: no visible characters, but its insertion style metadata travels inside
        // the same TextDisplayEntity.Data payload that Minecraft synchronizes to the client.
        MutableText markedText = Text.empty().setStyle(Style.EMPTY.withInsertion(TEXT_MARKER_PREFIX + entry.id()));
        markedText.append(Text.literal(entry.text()).styled(style -> style.withColor(entry.color() & 0xFFFFFF)));
        nbt.putString("text", Text.Serialization.toJsonString(markedText, world.getRegistryManager()));
        nbt.putString("billboard", "center");
        nbt.putFloat("view_range", 4.0F);
        nbt.putFloat("width", 2.0F);
        nbt.putFloat("height", 1.0F);
        nbt.putInt("line_width", 200);
        nbt.putByte("text_opacity", (byte) 0xFF);
        nbt.putByte("shadow", (byte) 1);
        nbt.putByte("see_through", (byte) 0);
        nbt.putByte("default_background", (byte) 0);
        nbt.putInt("background", 0x40000000);

        NbtCompound brightness = new NbtCompound();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        nbt.put("brightness", brightness);

        // TextDisplayEntity exposes its visual setters privately; its documented NBT is the vanilla
        // server-side configuration surface used by /summon and is synchronized by the entity itself.
        display.readNbt(nbt);
        display.refreshPositionAndAngles(entry.x(), entry.y() + VERTICAL_OFFSET, entry.z(), 0.0F, 0.0F);
        display.ignoreCameraFrustum = true;
        display.addCommandTag(tag(entry.id()));
        display.setCustomName(Text.literal("Emipokemon hologram " + entry.id()));
        display.setCustomNameVisible(false);
    }

    static boolean matches(DisplayEntity.TextDisplayEntity display, String id) {
        return display != null && display.getCommandTags().contains(tag(id));
    }

    static String tag(String id) {
        return TAG_PREFIX + HologramRegistryStore.normalize(id);
    }
}
