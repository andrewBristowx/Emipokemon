package com.emipokemon.npc;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.emipokemon.Emipokemon;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class NpcHealingService {
    private static final long COOLDOWN_MILLIS = 1_500L;
    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

    private NpcHealingService() {
    }

    static void heal(ServerPlayerEntity player, ServiceNpcEntity nurse) {
        long now = System.currentTimeMillis();
        long last = LAST_USE.getOrDefault(player.getUuid(), 0L);
        if (now - last < COOLDOWN_MILLIS) return;
        LAST_USE.put(player.getUuid(), now);

        PlayerPartyStore party;
        try {
            party = party(player);
        } catch (Exception exception) {
            Emipokemon.LOGGER.error("Could not access Cobblemon party for nurse NPC", exception);
            player.sendMessage(Text.literal("§cLa enfermera no pudo acceder a tu equipo. Inténtalo nuevamente."), false);
            return;
        }

        boolean hasPokemon = false;
        boolean neededHealing = false;
        for (Pokemon pokemon : party) {
            hasPokemon = true;
            if (!pokemon.isFullHealth() || pokemon.getStatus() != null || hasMissingPp(pokemon)) {
                neededHealing = true;
            }
            pokemon.heal();
        }

        if (!hasPokemon) {
            player.sendMessage(Text.literal("§dEnfermera Emi: §fAún no tienes Pokémon en tu equipo."), false);
            return;
        }

        if (neededHealing) {
            player.sendMessage(Text.literal("§dEnfermera Emi: §f¡Tu equipo quedó completamente recuperado! ♥"), false);
            ServerWorld world = (ServerWorld) nurse.getWorld();
            world.spawnParticles(ParticleTypes.HEART, nurse.getX(), nurse.getY() + 1.7, nurse.getZ(), 7, 0.35, 0.35, 0.35, 0.02);
            world.playSound(null, nurse.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.NEUTRAL, 0.8f, 1.25f);
        } else {
            player.sendMessage(Text.literal("§dEnfermera Emi: §fTu equipo ya está completamente sano. ♥"), false);
        }
    }

    private static boolean hasMissingPp(Pokemon pokemon) {
        for (Move move : pokemon.getMoveSet()) {
            if (move != null && move.getCurrentPp() < move.getMaxPp()) return true;
        }
        return false;
    }

    private static PlayerPartyStore party(ServerPlayerEntity player) throws ReflectiveOperationException {
        Object storage = Cobblemon.INSTANCE.getStorage();
        for (java.lang.reflect.Method method : storage.getClass().getMethods()) {
            if (method.getName().equals("getParty") && method.getParameterCount() == 1) {
                Object result = method.invoke(storage, player);
                if (result instanceof PlayerPartyStore party) return party;
            }
        }
        throw new NoSuchMethodException("Compatible Cobblemon getParty method was not found");
    }
}
