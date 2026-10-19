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

public final class Permissions {

    public static final ResourceLocation ATLANTIS_PERMISSIONS_HANDLER = ResourceLocation.fromNamespaceAndPath(Atlantis.ID, "permissions");

    public static UPermissionNode<?, Boolean> USER_GROUP_NODE;
    public static UPermissionNode<?, Boolean> ADMIN_GROUP_NODE;

    public static final String USER_GROUP_NAME = "user";
    public static final String ADMIN_GROUP_NAME = "admin";

    public static final String USER = Atlantis.ID + "." + USER_GROUP_NAME;
    public static final String ADMIN = Atlantis.ID + "." + ADMIN_GROUP_NAME;

    private static final Map<AtlantisPermissionGroup, UPermissionNode<?, Boolean>> groups = new HashMap<>();

    private Permissions() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

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

    public static void makeDefaultGroups() {
        Atlantis.CONFIG.put(new AtlantisPermissionGroup(USER_GROUP_NAME, USER_GROUP_NAME, DefaultPermissionUser.ALL, 0, "*." + USER_GROUP_NAME + ".+"));
        Atlantis.CONFIG.put(new AtlantisPermissionGroup(ADMIN_GROUP_NAME, ADMIN_GROUP_NAME, DefaultPermissionUser.OP, 2, "*." + ADMIN_GROUP_NAME + ".+"));
        Atlantis.CONFIG.save(AtlantisPermissionGroup.class);
    }

    public static boolean isOperator(Player player) {
        var server = ServerUtils.server();
        var list = ServerUtils.playerList();
        if (list == null) {
            return false;
        }
        return list.getOps().get(player.getGameProfile()) != null || (server != null && server.isSingleplayer() && player.isCreative());
    }

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

    public static boolean hasPermission(PlayerReference player, String node) {
        return player.entity().map(entity -> Permissions.hasPermission(entity, node)).orElse(false);
    }

    public static boolean hasPermission(Predicate<Integer> permissionLevel, String node) {
        for (var entry : groups.entrySet()) {
            if (entry.getKey().hasPermission(node) && permissionLevel.test(entry.getKey().permissionLevel())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canUse(Player player, String node) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean canUse(String node, Player player) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean hasPermission(String node, Player player) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean canUse(CommandSourceStack source, String node) {
        return Permissions.hasPermission(source, node);
    }

    public static boolean canUse(String node, CommandSourceStack source) {
        return Permissions.hasPermission(source, node);
    }

    public static boolean hasPermission(String node, CommandSourceStack source) {
        return Permissions.hasPermission(source, node);
    }

    public static boolean canUse(PlayerReference player, String node) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean canUse(String node, PlayerReference player) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean hasPermission(String node, PlayerReference player) {
        return Permissions.hasPermission(player, node);
    }

    public static boolean canUseCommand(Player sender, String domain, String command, boolean isAdmin, String... qualifiers) {
        return Permissions.canUse(Permissions.buildNode(domain, command, isAdmin, qualifiers), sender);
    }

    public static boolean canUseCommand(CommandSourceStack sender, String domain, String command, boolean isAdmin, String... qualifiers) {
        return Permissions.canUse(Permissions.buildNode(domain, command, isAdmin, qualifiers), sender);
    }

    public static String buildNode(String domain, String command, String... qualifiers) {
        return Permissions.buildNode(domain, command, false, qualifiers);
    }

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
