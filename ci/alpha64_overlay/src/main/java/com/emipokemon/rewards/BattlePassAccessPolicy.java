package com.emipokemon.rewards;

import com.emipokemon.config.EmipokemonConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.Locale;

/** Optional LuckPerms integration with safe operator, team and exact-name fallbacks. */
final class BattlePassAccessPolicy {
    private BattlePassAccessPolicy() { }

    static boolean hasPremium(ServerPlayerEntity player, EmipokemonConfig.BattlePassSettings settings) {
        if (player.hasPermissionLevel(2)) return true;
        String name = player.getGameProfile().getName().toLowerCase(Locale.ROOT);
        if (settings.premiumPlayerNames.contains(name)) return true;
        if (player.getScoreboardTeam() != null
                && settings.premiumGroups.contains(player.getScoreboardTeam().getName().toLowerCase(Locale.ROOT))) return true;
        return luckPermsAllows(player, settings);
    }

    private static boolean luckPermsAllows(ServerPlayerEntity player, EmipokemonConfig.BattlePassSettings settings) {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = provider.getMethod("get").invoke(null);
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUuid());
            if (user == null) return false;
            String group = String.valueOf(user.getClass().getMethod("getPrimaryGroup").invoke(user))
                    .toLowerCase(Locale.ROOT);
            if (settings.premiumGroups.contains(group)) return true;
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object result = permissionData.getClass().getMethod("checkPermission", String.class)
                    .invoke(permissionData, settings.premiumPermission);
            Method asBoolean = result.getClass().getMethod("asBoolean");
            return Boolean.TRUE.equals(asBoolean.invoke(result));
        } catch (Throwable unavailable) {
            return false;
        }
    }
}
