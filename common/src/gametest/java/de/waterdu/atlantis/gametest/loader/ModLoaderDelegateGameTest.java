package de.waterdu.atlantis.gametest.loader;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.util.List;

/**
 * Tests the {@code ModLoaderDelegate} of the running loader.
 */
public final class ModLoaderDelegateGameTest {

    /**
     * Prevents instantiation.
     */
    private ModLoaderDelegateGameTest() {}

    /**
     * Checks the server, the dedicated server flag, the config directory and {@code isModLoaded} of the delegate.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void environment(GameTestHelper helper) {
        var loader = Mods.modLoader();

        helper.assertTrue(loader.server() == helper.getLevel().getServer(), "server()");
        helper.assertTrue(loader.isDedicatedServer(), "isDedicatedServer()");
        helper.assertTrue(Files.isDirectory(loader.configDir()), "configDir() is not a directory");
        helper.assertTrue(loader.isModLoaded("atlantis"), "isModLoaded(atlantis)");
        helper.assertFalse(loader.isModLoaded("atlantis_does_not_exist"), "isModLoaded(unknown)");
        helper.succeed();
    }

    /**
     * Checks that {@code mods()} lists Atlantis with its display name and a version check.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void modsListsAtlantis(GameTestHelper helper) {
        var atlantis = Mods.modLoader().mods().stream().filter(mod -> mod.id().equals("atlantis")).findFirst().orElse(null);

        helper.assertTrue(atlantis != null, "mods() does not list atlantis");
        helper.assertValueEqual(atlantis.displayName(), "Atlantis", "display name");
        helper.assertTrue(atlantis.versionCheck() != null, "version check");
        helper.succeed();
    }

    /**
     * Checks that the annotation scan finds {@link Mods} and {@code AtlantisLevelRegistry} as {@link Bootstrapped}
     * classes.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void scanFindsBootstrappedClasses(GameTestHelper helper) {
        var scan = Mods.modLoader().scanAnnotatedClasses(List.of(Bootstrapped.class));

        var names = scan.get(Bootstrapped.class);
        helper.assertTrue(names != null && names.contains(Mods.class.getName()), "Mods is not found");
        helper.assertTrue(names.contains("de.waterdu.atlantis.level.level.AtlantisLevelRegistry"), "AtlantisLevelRegistry is not found");
        helper.succeed();
    }

    /**
     * Checks that the delegate does not report a mock player as a fake player.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void mockPlayersAreNotFakePlayers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        helper.assertFalse(Mods.modLoader().isFakePlayer(player), "isFakePlayer");
        helper.succeed();
    }

    /**
     * Checks that the block hooks allow a survival player to place and break dirt, and that the player cannot harvest
     * stone by hand.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void blockHooksAllowASurvivalPlayer(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        player.setGameMode(GameType.SURVIVAL);
        var dirt = helper.absolutePos(new BlockPos(1, 2, 1));
        var stone = helper.absolutePos(new BlockPos(2, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        var loader = Mods.modLoader();

        helper.assertFalse(loader.onBlockPlace(player, dirt.above()), "onBlockPlace reports a cancellation");
        helper.assertTrue(loader.onBlockBreak(player, dirt, Blocks.DIRT.defaultBlockState()), "onBlockBreak");
        helper.assertTrue(loader.onEntityDestroyBlock(player, dirt, Blocks.DIRT.defaultBlockState()), "onEntityDestroyBlock");
        helper.assertTrue(loader.canHarvest(player, dirt, Blocks.DIRT.defaultBlockState()), "canHarvest(dirt)");
        helper.assertFalse(loader.canHarvest(player, stone, Blocks.STONE.defaultBlockState()), "canHarvest(stone) by hand");
        helper.succeed();
    }

    /**
     * Checks that a boolean permission node has the namespace and the path, separated by {@code .}, as its key.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void createBooleanNodeHasItsKey(GameTestHelper helper) {
        var node = Mods.modLoader().createBooleanNode("atlantis_test", "node", player -> true);

        helper.assertValueEqual(node.key(), "atlantis_test.node", "node key");
        helper.succeed();
    }
}
