package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.neoforge.NeoForgeProxy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Starts Atlantis on NeoForge.
 */
@Mod(Atlantis.ID)
public class AtlantisNeoForge {

    private static AtlantisNeoForge INSTANCE;

    private final NeoForgeProxy proxy;

    /**
     * Stores the instance, creates the {@link NeoForgeProxy} and runs its setup, then adds the Atlantis listeners.
     *
     * @param modEventBus the event bus of the mod
     * @param modContainer the container of the mod
     */
    public AtlantisNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        this.proxy = new NeoForgeProxy(modEventBus, modContainer);
        this.proxy.setup();
        Atlantis.listeners();
    }

    /**
     * @return the instance of the mod, or {@code null} before NeoForge constructs it
     */
    public static AtlantisNeoForge instance() {
        return INSTANCE;
    }

    /**
     * @return the NeoForge implementation of the mod loader delegate
     */
    public NeoForgeProxy proxy() {
        return this.proxy;
    }
}
