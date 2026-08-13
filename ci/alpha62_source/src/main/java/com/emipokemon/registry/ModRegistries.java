package com.emipokemon.registry;

import com.emipokemon.Emipokemon;
import com.emipokemon.casino.CasinoGameType;
import com.emipokemon.casino.CasinoMachineBlock;
import com.emipokemon.casino.CasinoMachineBlockEntity;
import com.emipokemon.gacha.machine.GachaMachineBlock;
import com.emipokemon.gacha.machine.GachaMachineBlockEntity;
import com.emipokemon.gacha.machine.GachaMachineTopBlock;
import com.emipokemon.npc.ServiceNpcEntity;
import com.emipokemon.hologram.HologramEntity;
import com.emipokemon.visual.MediaDisplayEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRegistries {
    public static final EntityType<ServiceNpcEntity> NURSE_NPC = registerNpc("nurse_npc");
    public static final EntityType<ServiceNpcEntity> SHOP_NPC = registerNpc("shop_npc");
    public static final EntityType<ServiceNpcEntity> CUSTOM_NPC = registerNpc("custom_npc");
    public static final EntityType<ServiceNpcEntity> CUSTOM_SLIM_NPC = registerNpc("custom_slim_npc");
    public static final EntityType<MediaDisplayEntity> MEDIA_DISPLAY = Registry.register(
            Registries.ENTITY_TYPE, id("media_display"),
            EntityType.Builder.<MediaDisplayEntity>create(MediaDisplayEntity::new, SpawnGroup.MISC)
                    .dimensions(0.1f, 0.1f)
                    .maxTrackingRange(32)
                    .trackingTickInterval(20)
                    .build(Emipokemon.MOD_ID + ":media_display"));
    public static final EntityType<HologramEntity> HOLOGRAM = Registry.register(
            Registries.ENTITY_TYPE, id("hologram"),
            EntityType.Builder.<HologramEntity>create(HologramEntity::new, SpawnGroup.MISC)
                    .dimensions(0.1F, 0.1F)
                    .maxTrackingRange(48)
                    .trackingTickInterval(10)
                    .build(Emipokemon.MOD_ID + ":hologram"));

    public static final Item GACHA_TICKET = registerItem(
            "gacha_ticket",
            new Item(new Item.Settings().maxCount(64))
    );

    public static final Item EMI_SPECIAL_BANNER_TICKET = registerItem(
            "emi_special_banner_ticket",
            new Item(new Item.Settings().maxCount(64))
    );

    public static final Item CASINO_CHIP = registerItem("casino_chip", new Item(new Item.Settings().maxCount(64)));
    public static final Item CLAW_TICKET = registerItem("claw_ticket", new Item(new Item.Settings().maxCount(64)));

    public static final CasinoMachineBlock SLOT_MACHINE = casinoBlock("slot_machine");
    public static final CasinoMachineBlock MICHICOIN_CHIP_MACHINE = casinoBlock("michicoin_chip_machine");
    public static final CasinoMachineBlock TICKET_MACHINE = casinoBlock("ticket_machine");
    public static final CasinoMachineBlock CASINO_ROULETTE = casinoBlock("casino_roulette");
    public static final CasinoMachineBlock POKER_TABLE = casinoBlock("poker_table");
    public static final CasinoMachineBlock BLACKJACK_TABLE = casinoBlock("blackjack_table");
    public static final CasinoMachineBlock DICE_TABLE = casinoBlock("dice_table");
    public static final CasinoMachineBlock CLAW_MACHINE = casinoBlock("claw_machine");
    public static final CasinoMachineBlock POKEMON_WAGER_TABLE = casinoBlock("pokemon_wager_table");

    public static final GachaMachineBlock STANDARD_GACHA_MACHINE = registerBlockWithItem(
            "standard_gacha_machine",
            new GachaMachineBlock(AbstractBlock.Settings.create()
                    .strength(3.5f)
                    .luminance(state -> 5)
                    .nonOpaque())
    );

    public static final GachaMachineBlock EMI_GACHA_MACHINE = registerBlockWithItem(
            "emi_gacha_machine",
            new GachaMachineBlock(AbstractBlock.Settings.create()
                    .strength(3.5f)
                    .luminance(state -> 7)
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
            FabricBlockEntityTypeBuilder.create(
                    GachaMachineBlockEntity::new,
                    STANDARD_GACHA_MACHINE,
                    EMI_GACHA_MACHINE
            ).build()
    );

    public static final BlockEntityType<CasinoMachineBlockEntity> CASINO_MACHINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, id("casino_machine"),
            FabricBlockEntityTypeBuilder.create(CasinoMachineBlockEntity::new,
                    SLOT_MACHINE, MICHICOIN_CHIP_MACHINE, TICKET_MACHINE, CASINO_ROULETTE,
                    POKER_TABLE, BLACKJACK_TABLE, DICE_TABLE, CLAW_MACHINE,
                    POKEMON_WAGER_TABLE).build());

    private ModRegistries() {
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(NURSE_NPC, ServiceNpcEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(SHOP_NPC, ServiceNpcEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_NPC, ServiceNpcEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(CUSTOM_SLIM_NPC, ServiceNpcEntity.createMobAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(STANDARD_GACHA_MACHINE);
            entries.add(EMI_GACHA_MACHINE);
            entries.add(GACHA_TICKET);
            entries.add(EMI_SPECIAL_BANNER_TICKET);
            entries.add(CASINO_CHIP);
            entries.add(CLAW_TICKET);
            entries.add(SLOT_MACHINE);
            entries.add(MICHICOIN_CHIP_MACHINE);
            entries.add(TICKET_MACHINE);
            entries.add(CASINO_ROULETTE);
            entries.add(POKER_TABLE);
            entries.add(BLACKJACK_TABLE);
            entries.add(DICE_TABLE);
            entries.add(CLAW_MACHINE);
            entries.add(POKEMON_WAGER_TABLE);
        });
        Emipokemon.LOGGER.info("Emipokemon registries initialized: gacha, 9 casino blocks, 4 NPC types, media displays and holograms");
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

    private static CasinoMachineBlock casinoBlock(String path) {
        return registerBlockWithItem(path, new CasinoMachineBlock(AbstractBlock.Settings.create()
                .strength(3.5F).luminance(state -> 5).nonOpaque()));
    }

    public static CasinoGameType casinoType(Block block) {
        if (block == SLOT_MACHINE) return CasinoGameType.SLOT;
        if (block == MICHICOIN_CHIP_MACHINE) return CasinoGameType.CHIP_EXCHANGE;
        if (block == TICKET_MACHINE) return CasinoGameType.TICKET_EXCHANGE;
        if (block == CASINO_ROULETTE) return CasinoGameType.ROULETTE;
        if (block == POKER_TABLE) return CasinoGameType.POKER;
        if (block == BLACKJACK_TABLE) return CasinoGameType.BLACKJACK;
        if (block == DICE_TABLE) return CasinoGameType.DICE;
        if (block == CLAW_MACHINE) return CasinoGameType.CLAW;
        if (block == POKEMON_WAGER_TABLE) return CasinoGameType.POKEMON_FLIP;
        throw new IllegalArgumentException("Block is not a registered Emipokemon casino machine: " + block);
    }

    private static EntityType<ServiceNpcEntity> registerNpc(String path) {
        return Registry.register(Registries.ENTITY_TYPE, id(path),
                EntityType.Builder.create(ServiceNpcEntity::new, SpawnGroup.MISC)
                        .dimensions(0.6f, 1.8f)
                        .eyeHeight(1.62f)
                        .maxTrackingRange(10)
                        .trackingTickInterval(2)
                        .build(Emipokemon.MOD_ID + ":" + path));
    }
}
