package com.emipokemon.npc;

import com.emipokemon.Emipokemon;
import com.emipokemon.data.PlayerData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NpcRewardService {
    private NpcRewardService() {
    }

    public static List<String> validate(List<String> rawRewards) {
        if (rawRewards == null) return List.of();
        List<String> normalized = new ArrayList<>();
        for (String raw : rawRewards) {
            if (raw == null || raw.isBlank()) continue;
            if (normalized.size() >= 8) throw new IllegalArgumentException("Se permiten como máximo 8 recompensas.");
            ParsedReward reward = parse(raw);
            normalized.add(reward.itemId() + "*" + reward.count());
        }
        return List.copyOf(normalized);
    }

    public static boolean alreadyClaimed(ServerPlayerEntity player, ServiceNpcEntity npc) {
        if (npc.battleRewardRepeatable()) return false;
        return Emipokemon.playerDataManager().getOrLoad(player.getUuid()).claimedNpcRewards.contains(key(npc));
    }

    public static void grantAfterVictory(ServerPlayerEntity player, ServiceNpcEntity npc) {
        List<String> rewards;
        try {
            rewards = validate(npc.battleRewards());
        } catch (IllegalArgumentException exception) {
            Emipokemon.LOGGER.error("Invalid persisted rewards for NPC {}", npc.npcId(), exception);
            player.sendMessage(Text.literal("§cLa recompensa de este NPC está mal configurada."), false);
            return;
        }
        if (rewards.isEmpty()) return;

        PlayerData data = Emipokemon.playerDataManager().getOrLoad(player.getUuid());
        if (!npc.battleRewardRepeatable()) {
            if (!data.claimedNpcRewards.add(key(npc))) {
                player.sendMessage(Text.literal("§7Ya habías recibido la recompensa única de este entrenador."), false);
                return;
            }
            // Persist the reservation before any item enters the inventory.
            Emipokemon.playerDataManager().saveNow(player.getUuid());
        }

        for (String configured : rewards) {
            ParsedReward reward = parse(configured);
            ItemStack stack = new ItemStack(reward.item(), reward.count());
            if (!player.getInventory().insertStack(stack) && !stack.isEmpty()) player.dropItem(stack, false);
        }
        if (npc.battleRewardRepeatable()) Emipokemon.playerDataManager().saveNow(player.getUuid());
        player.sendMessage(Text.literal("§dVictoria: recibiste §f" + describe(rewards)
                + (npc.battleRewardRepeatable() ? " §7(repetible)" : " §7(recompensa única)")), false);
    }

    public static String describe(List<String> rewards) {
        if (rewards == null || rewards.isEmpty()) return "sin recompensa";
        return String.join(", ", rewards);
    }

    private static ParsedReward parse(String raw) {
        String value = raw.strip().toLowerCase(Locale.ROOT);
        int separator = value.lastIndexOf('*');
        String idText = separator < 0 ? value : value.substring(0, separator).strip();
        int count = 1;
        if (separator >= 0) {
            try {
                count = Integer.parseInt(value.substring(separator + 1).strip());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Cantidad inválida en " + raw);
            }
        }
        if (count < 1 || count > 64) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 64: " + raw);
        Identifier id = Identifier.tryParse(idText);
        if (id == null || !Registries.ITEM.containsId(id)) throw new IllegalArgumentException("Objeto inexistente: " + idText);
        Item item = Registries.ITEM.get(id);
        if (item == null || item.getDefaultStack().isEmpty()) throw new IllegalArgumentException("Objeto no entregable: " + idText);
        return new ParsedReward(id.toString(), item, count);
    }

    private static String key(ServiceNpcEntity npc) {
        return npc.getWorld().getRegistryKey().getValue() + ":" + npc.npcId();
    }

    private record ParsedReward(String itemId, Item item, int count) {
    }
}
