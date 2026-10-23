package de.waterdu.atlantis.gametest.fabric;

import de.waterdu.atlantis.gametest.AtlantisGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

import java.util.Collection;

/**
 * The {@code fabric-gametest} entrypoint. Fabric API registers this class with vanilla's GameTest registry.
 */
public final class AtlantisFabricGameTests {

    @GameTestGenerator
    public Collection<TestFunction> atlantisTests() {
        return AtlantisGameTests.collect();
    }
}
