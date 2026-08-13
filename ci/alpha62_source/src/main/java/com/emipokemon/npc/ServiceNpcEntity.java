package com.emipokemon.npc;

import com.emipokemon.registry.ModRegistries;
import com.emipokemon.shop.network.ShopNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ServiceNpcEntity extends MobEntity {
    private static final String NBT_NPC_ID = "EmipokemonNpcId";
    private static final String NBT_SHOP_CATEGORY = "EmipokemonShopCategory";
    private static final String NBT_DIALOGUE = "EmipokemonDialogue";
    private static final String NBT_TEAM = "EmipokemonPokemonTeam";
    private static final String NBT_REWARDS = "EmipokemonBattleRewards";
    private static final String NBT_REWARD_REPEATABLE = "EmipokemonBattleRewardRepeatable";
    private static final TrackedData<String> NPC_ID = DataTracker.registerData(
            ServiceNpcEntity.class, TrackedDataHandlerRegistry.STRING);

    private String shopCategory = "balls";
    private String dialogue = "";
    private final List<String> pokemonTeam = new ArrayList<>();
    private final List<String> battleRewards = new ArrayList<>();
    private boolean battleRewardRepeatable;

    public ServiceNpcEntity(EntityType<? extends ServiceNpcEntity> type, World world) {
        super(type, world);
        setAiDisabled(true);
        setInvulnerable(true);
        setPersistent();
        setCanPickUpLoot(false);
    }

    public String npcId() {
        return dataTracker.get(NPC_ID);
    }

    public void setNpcId(String npcId) {
        String normalized = npcId == null ? "" : npcId.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "");
        dataTracker.set(NPC_ID, normalized.length() > 32 ? normalized.substring(0, 32) : normalized);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(NPC_ID, "");
    }

    public String dialogue() {
        return dialogue;
    }

    public void setDialogue(String value) {
        String clean = value == null ? "" : value.strip();
        dialogue = clean.length() > 2048 ? clean.substring(0, 2048) : clean;
    }

    public List<String> pokemonTeam() {
        return List.copyOf(pokemonTeam);
    }

    public void setPokemonTeam(List<String> specs) {
        pokemonTeam.clear();
        if (specs == null) return;
        for (String spec : specs) {
            if (pokemonTeam.size() >= 6) break;
            String clean = spec == null ? "" : spec.strip();
            if (!clean.isBlank()) pokemonTeam.add(clean.length() > 256 ? clean.substring(0, 256) : clean);
        }
    }

    public List<String> battleRewards() {
        return List.copyOf(battleRewards);
    }

    public void setBattleRewards(List<String> rewards) {
        battleRewards.clear();
        if (rewards == null) return;
        for (String reward : rewards) {
            if (battleRewards.size() >= 8) break;
            String clean = reward == null ? "" : reward.strip();
            if (!clean.isBlank()) battleRewards.add(clean.length() > 128 ? clean.substring(0, 128) : clean);
        }
    }

    public boolean battleRewardRepeatable() {
        return battleRewardRepeatable;
    }

    public void setBattleRewardRepeatable(boolean repeatable) {
        battleRewardRepeatable = repeatable;
    }

    public String shopCategory() {
        return shopCategory;
    }

    public void setShopCategory(String category) {
        this.shopCategory = NpcKind.safeCategory(category);
    }

    public NpcKind kind() {
        if (getType() == ModRegistries.NURSE_NPC) return NpcKind.NURSE;
        if (getType() == ModRegistries.SHOP_NPC) return NpcKind.SHOP;
        return NpcKind.CUSTOM;
    }

    public boolean slimModel() {
        return getType() == ModRegistries.NURSE_NPC || getType() == ModRegistries.CUSTOM_SLIM_NPC;
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (getWorld().isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

        if (kind() == NpcKind.NURSE) {
            NpcHealingService.heal(serverPlayer, this);
        } else if (kind() == NpcKind.SHOP) {
            ShopNetworking.open(serverPlayer, shopCategory);
        } else if (serverPlayer.hasPermissionLevel(4) && player.isSneaking()) {
            NpcNetworking.openEditor(serverPlayer, this);
        } else {
            NpcNetworking.openDialogue(serverPlayer, this);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean collidesWith(net.minecraft.entity.Entity other) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        setVelocity(0.0, getVelocity().y, 0.0);
        setBodyYaw(getYaw());
        setHeadYaw(getYaw());
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString(NBT_NPC_ID, npcId());
        nbt.putString(NBT_SHOP_CATEGORY, shopCategory);
        nbt.putString(NBT_DIALOGUE, dialogue);
        NbtList team = new NbtList();
        for (String spec : pokemonTeam) team.add(NbtString.of(spec));
        nbt.put(NBT_TEAM, team);
        NbtList rewards = new NbtList();
        for (String reward : battleRewards) rewards.add(NbtString.of(reward));
        nbt.put(NBT_REWARDS, rewards);
        nbt.putBoolean(NBT_REWARD_REPEATABLE, battleRewardRepeatable);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setNpcId(nbt.getString(NBT_NPC_ID));
        setShopCategory(nbt.getString(NBT_SHOP_CATEGORY));
        setDialogue(nbt.getString(NBT_DIALOGUE));
        List<String> specs = new ArrayList<>();
        NbtList team = nbt.getList(NBT_TEAM, NbtString.STRING_TYPE);
        for (int index = 0; index < team.size() && index < 6; index++) specs.add(team.getString(index));
        setPokemonTeam(specs);
        List<String> rewards = new ArrayList<>();
        NbtList storedRewards = nbt.getList(NBT_REWARDS, NbtString.STRING_TYPE);
        for (int index = 0; index < storedRewards.size() && index < 8; index++) rewards.add(storedRewards.getString(index));
        setBattleRewards(rewards);
        setBattleRewardRepeatable(nbt.getBoolean(NBT_REWARD_REPEATABLE));
        setAiDisabled(true);
        setInvulnerable(true);
        setPersistent();
        setCustomNameVisible(true);
    }

    public enum NpcKind {
        NURSE("enfermera", "Enfermera Emi"),
        SHOP("tienda", "Vendedor Poké Mart"),
        CUSTOM("custom", "NPC personalizado");

        private final String commandName;
        private final String defaultDisplayName;

        NpcKind(String commandName, String defaultDisplayName) {
            this.commandName = commandName;
            this.defaultDisplayName = defaultDisplayName;
        }

        public String commandName() {
            return commandName;
        }

        public Text defaultDisplayName() {
            return Text.literal(defaultDisplayName);
        }

        public static String safeCategory(String category) {
            if (category == null || category.isBlank()) return "balls";
            String normalized = category.trim().toLowerCase(java.util.Locale.ROOT);
            return switch (normalized) {
                case "balls", "medicine", "battle", "evolution", "supplies", "special_balls", "special_evolution", "protections", "gacha" -> normalized;
                default -> "balls";
            };
        }
    }
}
