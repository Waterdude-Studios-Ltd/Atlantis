package de.waterdu.atlantis.permission;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseCommandBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Checks string permissions against the Atlantis permission groups and their loader permission nodes.
 */
public final class Permissions {

    public static final ResourceLocation ATLANTIS_PERMISSIONS_HANDLER = ResourceLocation.fromNamespaceAndPath(Atlantis.ID, "permissions");

    /**
     * null until {@link #buildNodes(Consumer)} runs.
     */
    public static UPermissionNode<?, Boolean> USER_GROUP_NODE;
    /**
     * null until {@link #buildNodes(Consumer)} runs.
     */
    public static UPermissionNode<?, Boolean> ADMIN_GROUP_NODE;

    public static final String USER_GROUP_NAME = "user";
    public static final String ADMIN_GROUP_NAME = "admin";

    public static final String USER = Atlantis.ID + "." + USER_GROUP_NAME;
    public static final String ADMIN = Atlantis.ID + "." + ADMIN_GROUP_NAME;

    private static final Map<AtlantisPermissionGroup, UPermissionNode<?, Boolean>> groups = new HashMap<>();

    private Permissions() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Makes a node for each permission group and gives it to {@code sink}. Makes the default groups first if there are no groups.
     * The loaders call this.
     */
    public static void buildNodes(Consumer<UPermissionNode<?, Boolean>> sink) {
        if (Atlantis.CONFIG.isEmpty(AtlantisPermissionGroup.class)) {
            Permissions.makeDefaultGroups();
        }

        groups.clear();
        for (var group : Atlantis.CONFIG.all(AtlantisPermissionGroup.class)) {
            var node = group.makeNode();
            sink.accept(node);
            groups.put(group, node);

            if (group.name().equalsIgnoreCase(USER_GROUP_NAME)) {
                USER_GROUP_NODE = node;
            } else if (group.name().equalsIgnoreCase(ADMIN_GROUP_NAME)) {
                ADMIN_GROUP_NODE = node;
            }
        }
    }

    /**
     * Adds and saves the {@code user} group, which all players have by default, and the {@code admin} group, which
     * operators have by default.
     */
    public static void makeDefaultGroups() {
        Atlantis.CONFIG.put(new AtlantisPermissionGroup(USER_GROUP_NAME, USER_GROUP_NAME, DefaultPermissionUser.ALL, 0, "*." + USER_GROUP_NAME + ".+"));
        Atlantis.CONFIG.put(new AtlantisPermissionGroup(ADMIN_GROUP_NAME, ADMIN_GROUP_NAME, DefaultPermissionUser.OP, 2, "*." + ADMIN_GROUP_NAME + ".+"));
        Atlantis.CONFIG.save(AtlantisPermissionGroup.class);
    }

    /**
     * @return {@code true} if the player is on the operator list, or is in creative mode in singleplayer. {@code false}
     *     when no server runs.
     */
    public static boolean isOperator(Player player) {
        var server = ServerUtils.server();
        var list = ServerUtils.playerList();
        if (list == null) {
            return false;
        }
        return list.getOps().get(player.getGameProfile()) != null || (server != null && server.isSingleplayer() && player.isCreative());
    }

    /**
     * @param node the permission to check. The patterns of the permission groups match it.
     * @return {@code true} if a group matches the permission and the player has the node of that group. Always
     *     {@code true} for players that are not server players.
     */
    public static boolean hasPermission(Player player, String node) {
        if (player instanceof ServerPlayer serverPlayer) {
            for (var entry : groups.entrySet()) {
                if (entry.getKey().hasPermission(node) && Boolean.TRUE.equals(entry.getValue().get(serverPlayer))) {
                    return true;
                }
            }
        } else {
            // client
            return true;
        }
        return false;
    }

    /**
     * Server players are checked with {@link #hasPermission(Player, String)}. Other entities and command blocks are checked
     * against the permission level of the matching groups. Command blocks have level 2.
     *
     * @return always {@code true} for the server. Always {@code false} for sources that are not entities, command
     *     blocks or the server.
     */
    public static boolean hasPermission(CommandSource source, String node) {
        if (source instanceof Entity entity) {
            if (entity instanceof ServerPlayer serverPlayer) {
                return hasPermission(serverPlayer, node);
            } else {
                return hasPermission(entity::hasPermissions, node);
            }
        } else if (source instanceof BaseCommandBlock) {
            return hasPermission(level -> level <= 2, node);
        } else return source instanceof MinecraftServer;
    }

    /**
     * Players are checked with {@link #hasPermission(Player, String)}. Other sources are checked against the permission level
     * of the matching groups.
     */
    public static boolean hasPermission(CommandSourceStack source, String node) {
        if (source.isPlayer()) {
            return hasPermission(source.getPlayer(), node);
        } else {
            for (var entry : groups.entrySet()) {
                if (entry.getKey().hasPermission(node) && source.hasPermission(entry.getKey().permissionLevel())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return {@code false} when the player is offline
     */
    public static boolean hasPermission(PlayerReference player, String node) {
        return player.entity().map(entity -> Permissions.hasPermission(entity, node)).orElse(false);
    }

    /**
     * @param permissionLevel tests the permission level of each group that matches the permission
     * @return {@code true} if the test passes for one of those groups
     */
    public static boolean hasPermission(Predicate<Integer> permissionLevel, String node) {
        for (var entry : groups.entrySet()) {
            if (entry.getKey().hasPermission(node) && permissionLevel.test(entry.getKey().permissionLevel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the permission with {@link #hasPermission(Player, String)}.
     *
     * @param player the player to check
     * @param node the permission to check
     * @return {@code true} if the player has the permission
     */
    public static boolean canUse(Player player, String node) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(Player, String)}.
     *
     * @param node the permission to check
     * @param player the player to check
     * @return {@code true} if the player has the permission
     */
    public static boolean canUse(String node, Player player) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(Player, String)}.
     *
     * @param node the permission to check
     * @param player the player to check
     * @return {@code true} if the player has the permission
     */
    public static boolean hasPermission(String node, Player player) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(CommandSourceStack, String)}.
     *
     * @param source the command source to check
     * @param node the permission to check
     * @return {@code true} if the source has the permission
     */
    public static boolean canUse(CommandSourceStack source, String node) {
        return Permissions.hasPermission(source, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(CommandSourceStack, String)}.
     *
     * @param node the permission to check
     * @param source the command source to check
     * @return {@code true} if the source has the permission
     */
    public static boolean canUse(String node, CommandSourceStack source) {
        return Permissions.hasPermission(source, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(CommandSourceStack, String)}.
     *
     * @param node the permission to check
     * @param source the command source to check
     * @return {@code true} if the source has the permission
     */
    public static boolean hasPermission(String node, CommandSourceStack source) {
        return Permissions.hasPermission(source, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(PlayerReference, String)}.
     *
     * @param player the player to check
     * @param node the permission to check
     * @return {@code true} if the player has the permission. {@code false} when the player is offline.
     */
    public static boolean canUse(PlayerReference player, String node) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(PlayerReference, String)}.
     *
     * @param node the permission to check
     * @param player the player to check
     * @return {@code true} if the player has the permission. {@code false} when the player is offline.
     */
    public static boolean canUse(String node, PlayerReference player) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks the permission with {@link #hasPermission(PlayerReference, String)}.
     *
     * @param node the permission to check
     * @param player the player to check
     * @return {@code true} if the player has the permission. {@code false} when the player is offline.
     */
    public static boolean hasPermission(String node, PlayerReference player) {
        return Permissions.hasPermission(player, node);
    }

    /**
     * Checks if the player has the permission node of the command. See
     * {@link #buildNode(String, String, boolean, String...)}.
     *
     * @param sender the player to check
     * @param domain the first segment of the node, usually the mod ID
     * @param command the name of the command
     * @param isAdmin {@code true} for an {@code admin} node, {@code false} for a {@code user} node
     * @param qualifiers the segments to add after the command
     * @return {@code true} if the player has the permission
     */
    public static boolean canUseCommand(Player sender, String domain, String command, boolean isAdmin, String... qualifiers) {
        return Permissions.canUse(Permissions.buildNode(domain, command, isAdmin, qualifiers), sender);
    }

    /**
     * Checks if the command source has the permission node of the command. See
     * {@link #buildNode(String, String, boolean, String...)}.
     *
     * @param sender the command source to check
     * @param domain the first segment of the node, usually the mod ID
     * @param command the name of the command
     * @param isAdmin {@code true} for an {@code admin} node, {@code false} for a {@code user} node
     * @param qualifiers the segments to add after the command
     * @return {@code true} if the source has the permission
     */
    public static boolean canUseCommand(CommandSourceStack sender, String domain, String command, boolean isAdmin, String... qualifiers) {
        return Permissions.canUse(Permissions.buildNode(domain, command, isAdmin, qualifiers), sender);
    }

    /**
     * Builds a {@code user} permission node for the command.
     *
     * @param domain the first segment of the node, usually the mod ID
     * @param command the name of the command
     * @param qualifiers the segments to add after the command
     * @return {@code <domain>.user.<command>}, followed by each qualifier after a dot
     */
    public static String buildNode(String domain, String command, String... qualifiers) {
        return Permissions.buildNode(domain, command, false, qualifiers);
    }

    /**
     * @return {@code <domain>.admin.<command>} or {@code <domain>.user.<command>},
     *     followed by each qualifier after a dot
     */
    public static String buildNode(String domain, String command, boolean isAdmin, String... qualifiers) {
        var builder = new StringBuilder();
        builder.append(domain).append(".").append(isAdmin ? ADMIN_GROUP_NAME : USER_GROUP_NAME).append(".").append(command);
        if (qualifiers != null) {
            for (var qualifier : qualifiers) {
                builder.append(".").append(qualifier);
            }
        }
        return builder.toString();
    }
}
