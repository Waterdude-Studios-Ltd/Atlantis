package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestLevels;
import de.waterdu.atlantis.level.level.implementation.AtlantisChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;

/**
 * GameTestLevels registers an Atlantis level through REGISTRY_INJECT before the server loads its registries.
 * A GameTest server builds its dimensions only from the flat preset. Thus, the server creates the level stem, but it
 * does not make a ServerLevel from it. A normal server makes the ServerLevel.
 */
public final class RegistryInjectGameTest {

    /**
     * Prevents instantiation.
     */
    private RegistryInjectGameTest() {}

    /**
     * Checks that the dimension type registry contains the injected dimension type and that the level holds it.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void dimensionTypeIsInjected(GameTestHelper helper) {
        var registry = helper.getLevel().getServer().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        var key = ResourceKey.create(Registries.DIMENSION_TYPE, GameTestLevels.INJECTED_KEY.location());

        helper.assertTrue(registry.containsKey(key), "dimension type was not injected");
        var holder = GameTestLevels.injected().dimensionType();
        helper.assertTrue(holder != null && holder.is(key), "the level's dimension type holder");
        helper.succeed();
    }

    /**
     * Checks that the level stem exists and has an {@link AtlantisChunkGenerator} and the dimension type of the level.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void levelStemIsBuiltWithItsGenerator(GameTestHelper helper) {
        var stem = GameTestLevels.injected().levelStem();

        helper.assertTrue(stem != null, "level stem was not injected");
        helper.assertTrue(stem.value().generator() instanceof AtlantisChunkGenerator, "level stem has the wrong generator");
        helper.assertTrue(stem.value().type() == GameTestLevels.injected().dimensionType(), "level stem has the wrong dimension type");
        helper.succeed();
    }
}
