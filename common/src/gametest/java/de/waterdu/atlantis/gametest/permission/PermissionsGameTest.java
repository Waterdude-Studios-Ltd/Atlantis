package de.waterdu.atlantis.gametest.permission;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.permission.DefaultPermissionUser;
import de.waterdu.atlantis.permission.Permissions;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Atlantis' default groups: "user" (everyone, permission level 0) grants *.user.+, "admin" (operators, level 2)
 * grants *.admin.+.
 */
public final class PermissionsGameTest {

    private static final String USER_NODE = Permissions.buildNode("atlantis_test", "command", false);
    private static final String ADMIN_NODE = Permissions.buildNode("atlantis_test", "command", true);

    private PermissionsGameTest() {}

    @AtlantisGameTest
    public static void playersHaveUserPermissions(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        helper.assertFalse(Permissions.isOperator(player), "isOperator");
        helper.assertTrue(Permissions.hasPermission(player, USER_NODE), "user node");
        helper.assertFalse(Permissions.hasPermission(player, ADMIN_NODE), "admin node");
        helper.assertTrue(Permissions.canUse(player, Permissions.USER), "canUse(atlantis.user)");
        helper.assertTrue(Permissions.canUseCommand(player, "atlantis_test", "command", false), "canUseCommand(user)");
        helper.assertFalse(Permissions.canUseCommand(player, "atlantis_test", "command", true), "canUseCommand(admin)");
        helper.assertTrue(player.hasPermission(USER_NODE), "PlayerExt user node");
        helper.assertFalse(player.hasPermission(ADMIN_NODE), "PlayerExt admin node");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void operatorsHaveAdminPermissions(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        op(helper, player, true);
        try {
            helper.assertTrue(Permissions.isOperator(player), "isOperator");
            helper.assertTrue(Permissions.hasPermission(player, ADMIN_NODE), "admin node");
            helper.assertTrue(Permissions.hasPermission(player, USER_NODE), "user node");
            helper.assertTrue(Permissions.canUseCommand(player, "atlantis_test", "command", true), "canUseCommand(admin)");
        } finally {
            op(helper, player, false);
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void defaultPermissionUsers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var operator = GameTestPlayers.mock(helper);
        op(helper, operator, true);
        try {
            helper.assertTrue(DefaultPermissionUser.ALL.test(player), "ALL");
            helper.assertFalse(DefaultPermissionUser.NONE.test(operator), "NONE");
            helper.assertFalse(DefaultPermissionUser.OP.test(player), "OP for a player");
            helper.assertTrue(DefaultPermissionUser.OP.test(operator), "OP for an operator");
        } finally {
            op(helper, operator, false);
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void groupNodesResolveThroughTheLoader(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var operator = GameTestPlayers.mock(helper);
        op(helper, operator, true);
        try {
            helper.assertTrue(Permissions.USER_GROUP_NODE != null && Permissions.ADMIN_GROUP_NODE != null, "group nodes were not built");
            helper.assertValueEqual(Permissions.USER_GROUP_NODE.get(player), Boolean.TRUE, "user group node");
            helper.assertValueEqual(Permissions.ADMIN_GROUP_NODE.get(player), Boolean.FALSE, "admin group node for a player");
            helper.assertValueEqual(Permissions.ADMIN_GROUP_NODE.get(operator), Boolean.TRUE, "admin group node for an operator");
            helper.assertValueEqual(player.hasPermission(Permissions.USER_GROUP_NODE), Boolean.TRUE, "PlayerExt node");
        } finally {
            op(helper, operator, false);
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void newNodesResolveOnlyOnFabric(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var node = Mods.modLoader().createBooleanNode("atlantis_test", "unregistered", it -> it == player);

        if (Mods.isPresent(Mods.NEOFORGE_ID)) {
            try {
                node.get(player);
                helper.fail("NeoForge resolved an unregistered node");
            } catch (RuntimeException expected) {
            }
        } else {
            helper.assertValueEqual(node.get(player), Boolean.TRUE, "default resolver");
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void commandSourcePermissions(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = GameTestPlayers.mock(helper);
        var playerSource = player.createCommandSourceStack();
        var console = server.createCommandSourceStack();

        helper.assertTrue(playerSource.hasPermission(USER_NODE), "player source user node");
        helper.assertFalse(playerSource.hasPermission(ADMIN_NODE), "player source admin node");
        helper.assertValueEqual(playerSource.hasPermission(Permissions.USER_GROUP_NODE), Boolean.TRUE, "player source group node");
        helper.assertTrue(console.hasPermission(ADMIN_NODE), "console admin node");
        helper.assertTrue(console.<Boolean>hasPermission(Permissions.USER_GROUP_NODE) == null, "console node checks must return null");
        helper.succeed();
    }

    private static void op(GameTestHelper helper, ServerPlayer player, boolean op) {
        var list = helper.getLevel().getServer().getPlayerList();
        if (op) {
            list.op(player.getGameProfile());
        } else {
            list.deop(player.getGameProfile());
        }
    }
}
