package com.emipokemon.registry;

import com.emipokemon.Emipokemon;
import com.emipokemon.gacha.machine.GachaMachineBlock;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import com.emipokemon.gacha.machine.GachaMachineTopBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRegistries {
    public static final Item GACHA_TICKET = registerItem(
            "gacha_ticket",
            new Item(new Item.Settings().maxCount(64))
    );

    public static final Item EMI_SPECIAL_BANNER_TICKET = registerItem(
            "emi_special_banner_ticket",
            new Item(new Item.Settings().maxCount(64))
    );

    public static final GachaMachineBlock STANDARD_GACHA_MACHINE = registerBlockWithItem(
            "standard_gacha_machine",
            new GachaMachineBlock(AbstractBlock.Settings.create()
                    .strength(3.5f)
                    .luminance(state -> 5)
                    .nonOpaque())
    );

    public static final GachaMachineTopBlock GACHA_MACHINE_TOP = registerBlock(
            "gacha_machine_top",
            new GachaMachineTopBlock(AbstractBlock.Settings.create()
                    .strength(3.5f)
                    .nonOpaque())
    );

    public static final BlockEntityType<GachaMachineBlockEntity> GACHA_MACHINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("standard_gacha_machine"),
            FabricBlockEntityTypeBuilder.create(GachaMachineBlockEntity::new, STANDARD_GACHA_MACHINE).build()
    );

    private ModRegistries() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(STANDARD_GACHA_MACHINE);
            entries.add(GACHA_TICKET);
            entries.add(EMI_SPECIAL_BANNER_TICKET);
        });
        Emipokemon.LOGGER.info("Emipokemon Phase 3 registries initialized: machine + 2 tickets");
    }

    private static Identifier id(String path) {
        return Identifier.of(Emipokemon.MOD_ID, path);
    }

    private static Item registerItem(String path, Item item) {
        return Registry.register(Registries.ITEM, id(path), item);
    }

    private static <T extends Block> T registerBlock(String path, T block) {
        return Registry.register(Registries.BLOCK, id(path), block);
    }

    private static <T extends Block> T registerBlockWithItem(String path, T block) {
        registerBlock(path, block);
        Registry.register(Registries.ITEM, id(path), new BlockItem(block, new Item.Settings()));
        return block;
    }
}
