package de.waterdu.atlantis.gametest.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AtlantisCommandGameTest {

    private static final List<String> SUBCOMMANDS = List.of("mods", "reload", "logpages", "schematics", "decorate", "testpage", "levelinfo", "goto");

    private AtlantisCommandGameTest() {}

    @AtlantisGameTest
    public static void commandTreeIsRegistered(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("atlantis");

        helper.assertTrue(root != null, "/atlantis is not registered");
        for (var name : SUBCOMMANDS) {
            helper.assertTrue(root.getChild(name) != null, "/atlantis " + name + " is not registered");
        }
        var schematics = root.getChild("schematics");
        for (var name : List.of("undo", "reload", "place")) {
            helper.assertTrue(schematics.getChild(name) != null, "/atlantis schematics " + name + " is not registered");
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void usersCanRunOnlyTheRoot(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestPlayers.mock(helper);
        var source = player.createCommandSourceStack();

        execute(helper, "atlantis", source);
        for (var name : SUBCOMMANDS) {
            try {
                execute(helper, "atlantis " + name, source);
                helper.fail("a non-operator could run /atlantis " + name);
            } catch (CommandSyntaxException expected) {
            }
        }
        helper.succeed();
    }

    @AtlantisGameTest
    public static void adminSubcommandsRun(GameTestHelper helper) throws CommandSyntaxException {
        var player = operator(helper);
        try {
            var source = player.createCommandSourceStack();
            execute(helper, "atlantis mods", source);
            execute(helper, "atlantis reload", source);
            execute(helper, "atlantis schematics reload", source);
            execute(helper, "atlantis schematics undo", source);
            execute(helper, "atlantis levelinfo", source);
            execute(helper, "atlantis logpages", source);
            execute(helper, "atlantis logpages", source);
            var page = java.util.Arrays.stream(Atlantis.listeners().pageNames()).findFirst();
            if (page.isPresent()) {
                execute(helper, "atlantis decorate \"" + page.get() + "\"", source);
                player.closeContainer();
            }
        } finally {
            deop(helper, player);
        }
        helper.succeed();
    }

    @AtlantisGameTest(timeoutTicks = 100000)
    public static void testPageOpens(GameTestHelper helper) throws CommandSyntaxException {
        var player = operator(helper);
        try {
            execute(helper, "atlantis testpage", player.createCommandSourceStack());
        } finally {
            deop(helper, player);
        }
        helper.succeedWhen(() -> helper.assertTrue(player.containerMenu != player.inventoryMenu, "the test page did not open"));
    }

    @AtlantisGameTest
    public static void gotoTeleportsToTheDimension(GameTestHelper helper) throws CommandSyntaxException {
        var player = operator(helper);
        try {
            execute(helper, "atlantis goto minecraft:the_nether", player.createCommandSourceStack());

            helper.assertTrue(player.level().dimension() == Level.NETHER, "player was not teleported");
        } finally {
            deop(helper, player);
        }
        helper.succeed();
    }

    private static int execute(GameTestHelper helper, String command, CommandSourceStack source) throws CommandSyntaxException {
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static ServerPlayer operator(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
        return player;
    }

    private static void deop(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().deop(player.getGameProfile());
    }
}
