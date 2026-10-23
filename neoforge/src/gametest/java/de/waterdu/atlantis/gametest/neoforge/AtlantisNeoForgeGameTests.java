package de.waterdu.atlantis.gametest.neoforge;

import de.waterdu.atlantis.gametest.AtlantisGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;

/**
 * The GameTest mod. Its tests only run when {@code neoforge.enabledGameTestNamespaces} includes its namespace.
 */
@Mod(AtlantisGameTests.NAMESPACE)
public final class AtlantisNeoForgeGameTests {

    public AtlantisNeoForgeGameTests(IEventBus modEventBus) {
        modEventBus.addListener(RegisterGameTestsEvent.class, event -> event.register(Generator.class));
    }

    @GameTestHolder(AtlantisGameTests.NAMESPACE)
    public static final class Generator {

        @GameTestGenerator
        public Collection<TestFunction> atlantisTests() {
            return AtlantisGameTests.collect();
        }
    }
}
