package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestLevels;
import de.waterdu.atlantis.level.level.implementation.AtlantisChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;

/**
 * GameTestLevels registers an Atlantis level through REGISTRY_INJECT before the server loads its registries.
 * A GameTest server builds its dimensions from the flat preset alone, so the level stem is created but no
 * ServerLevel is made from it, unlike on a normal server.
 */
public final class RegistryInjectGameTest {

    private RegistryInjectGameTest() {}

    @AtlantisGameTest
    public static void dimensionTypeIsInjected(GameTestHelper helper) {
        var registry = helper.getLevel().getServer().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        var key = ResourceKey.create(Registries.DIMENSION_TYPE, GameTestLevels.INJECTED_KEY.location());

        helper.assertTrue(registry.containsKey(key), "dimension type was not injected");
        var holder = GameTestLevels.injected().dimensionType();
        helper.assertTrue(holder != null && holder.is(key), "the level's dimension type holder");
        helper.succeed();
    }

    @AtlantisGameTest
    public static void levelStemIsBuiltWithItsGenerator(GameTestHelper helper) {
        var stem = GameTestLevels.injected().levelStem();

        helper.assertTrue(stem != null, "level stem was not injected");
        helper.assertTrue(stem.value().generator() instanceof AtlantisChunkGenerator, "level stem has the wrong generator");
        helper.assertTrue(stem.value().type() == GameTestLevels.injected().dimensionType(), "level stem has the wrong dimension type");
        helper.succeed();
    }
}
