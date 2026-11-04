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

/**
 * Tests the {@code /atlantis} command and its subcommands.
 */
public final class AtlantisCommandGameTest {

    private static final List<String> SUBCOMMANDS = List.of("mods", "reload", "logpages", "schematics", "decorate", "testpage", "levelinfo", "goto");

    /**
     * Prevents instantiation.
     */
    private AtlantisCommandGameTest() {}

    /**
     * Checks that {@code /atlantis}, all of its subcommands and the {@code schematics} subcommands are registered.
     *
     * @param helper the helper of the running test
     */
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

    /**
     * Checks that a player who is not an operator can run {@code /atlantis}, but none of its subcommands.
     *
     * @param helper the helper of the running test
     * @throws CommandSyntaxException if the player cannot run {@code /atlantis}
     */
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

    /**
     * Checks that an operator can run the admin subcommands. If a UI page exists, it also runs
     * {@code /atlantis decorate} for the first page.
     *
     * @param helper the helper of the running test
     * @throws CommandSyntaxException if a subcommand fails to run
     */
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

    /**
     * Checks that {@code /atlantis testpage} opens a container for the operator.
     *
     * @param helper the helper of the running test
     * @throws CommandSyntaxException if the command fails to run
     */
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

    /**
     * Checks that {@code /atlantis goto minecraft:the_nether} teleports the operator to the Nether.
     *
     * @param helper the helper of the running test
     * @throws CommandSyntaxException if the command fails to run
     */
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

    /**
     * Runs a command through the dispatcher of the server.
     *
     * @param helper the helper of the running test
     * @param command the command, without the leading {@code /}
     * @param source the source that runs the command
     * @return the result of the command
     * @throws CommandSyntaxException if the command cannot be parsed or the source cannot run it
     */
    private static int execute(GameTestHelper helper, String command, CommandSourceStack source) throws CommandSyntaxException {
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    /**
     * Creates a mock player and makes it an operator.
     *
     * @param helper the helper of the running test
     * @return the operator
     */
    private static ServerPlayer operator(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
        return player;
    }

    /**
     * Removes the operator status from the player.
     *
     * @param helper the helper of the running test
     * @param player the player to remove the status from
     */
    private static void deop(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().deop(player.getGameProfile());
    }
}
