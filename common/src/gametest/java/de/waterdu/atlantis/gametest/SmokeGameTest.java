package de.waterdu.atlantis.gametest;

import de.waterdu.atlantis.mod.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Checks that the GameTest wiring works and that Atlantis bootstrapped on the running loader.
 */
public final class SmokeGameTest {

    private SmokeGameTest() {}

    @AtlantisGameTest
    public static void structureLoads(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 0));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
        helper.succeed();
    }

    @AtlantisGameTest
    public static void atlantisBootstrapped(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        helper.assertTrue(Mods.modLoader().server() == server, "Mods.modLoader().server() is not the running server");
        helper.assertTrue(Mods.modLoader().isModLoaded("atlantis"), "Atlantis is not reported as loaded");
        helper.assertTrue(server.getCommands().getDispatcher().getRoot().getChild("atlantis") != null, "/atlantis is not registered");
        helper.succeed();
    }
}
