package com.emipokemon.progress;

import com.emipokemon.Emipokemon;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Resolves simultaneous-job slots without creating a hard LuckPerms dependency. */
public final class JobAccessPolicy {
    public static final String FOUR_JOBS_PERMISSION = "emipokemon.jobs.limit.4";
    public static final String ALL_JOBS_PERMISSION = "emipokemon.jobs.limit.all";
    private static final int SUPPORTER_LIMIT = 4;
    private static boolean luckPermsWarningLogged;

    private JobAccessPolicy() {
    }

    public static int maxActiveJobs(ServerPlayerEntity player) {
        if (hasFabricPermission(player, ALL_JOBS_PERMISSION)) return JobType.values().length;
        if (hasFabricPermission(player, FOUR_JOBS_PERMISSION)) return SUPPORTER_LIMIT;
        return JobLimitRules.forGroups(groupsFor(player), JobType.values().length);
    }

    /** Checks an optional LuckPerms/Fabric Permissions node without requiring either API at runtime. */
    public static boolean hasPermission(ServerPlayerEntity player, String node) {
        if (node == null || node.isBlank()) return true;
        if (hasFabricPermission(player, node)) return true;
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsClass.getMethod("getUserManager").invoke(luckPerms);
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerClass.getMethod("getUser", UUID.class).invoke(userManager, player.getUuid());
            if (user == null) return false;
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object tristate = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, node);
            Object allowed = tristate.getClass().getMethod("asBoolean").invoke(tristate);
            return allowed instanceof Boolean value && value;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emipokemon.LOGGER.debug("Could not query optional LuckPerms permission {}", node, exception);
            return false;
        }
    }

    private static boolean hasFabricPermission(ServerPlayerEntity player, String node) {
        try {
            Class<?> permissions = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            ServerCommandSource source = player.getCommandSource();
            for (Object permissionSubject : new Object[]{source, player}) {
                for (Method method : permissions.getMethods()) {
                    if (!method.getName().equals("check") || method.getParameterCount() != 2) continue;
                    if (!method.getParameterTypes()[0].isInstance(permissionSubject)
                            || method.getParameterTypes()[1] != String.class) continue;
                    Object result = method.invoke(null, permissionSubject, node);
                    if (result instanceof Boolean allowed) return allowed;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // LuckPerms group lookup below remains available when this optional bridge is absent.
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emipokemon.LOGGER.debug("Could not query optional permission node {}", node, exception);
        }
        return false;
    }

    public static Set<String> groupsFor(ServerPlayerEntity player) {
        return Set.copyOf(resolveGroups(player.getUuid()));
    }

    private static Set<String> resolveGroups(UUID playerId) {
        Set<String> groups = new LinkedHashSet<>();
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object userManager = luckPermsClass.getMethod("getUserManager").invoke(luckPerms);
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            Object user = userManagerClass.getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) return groups;
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            Object group = userClass.getMethod("getPrimaryGroup").invoke(user);
            if (group != null) groups.add(group.toString());
            Object nodes = userClass.getMethod("getNodes").invoke(user);
            if (nodes instanceof Iterable<?> iterable) {
                Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                Method getKey = nodeClass.getMethod("getKey");
                for (Object node : iterable) {
                    Object key = getKey.invoke(node);
                    if (key == null) continue;
                    String value = key.toString();
                    if (value.startsWith("group.")) groups.add(value.substring("group.".length()));
                }
            }
            return groups;
        } catch (ClassNotFoundException ignored) {
            return groups;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!luckPermsWarningLogged) {
                luckPermsWarningLogged = true;
                Emipokemon.LOGGER.warn("LuckPerms groups could not be read; Emipokemon will use default rank rules", exception);
            }
            return groups;
        }
    }
}
