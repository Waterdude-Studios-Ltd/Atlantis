package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.fabric.FabricProxy;
import net.fabricmc.api.ModInitializer;

/**
 * Starts Atlantis on Fabric.
 */
public class AtlantisFabric implements ModInitializer {

    public static AtlantisFabric INSTANCE;

    private FabricProxy proxy;

    /**
     * Stores the instance, creates the {@link FabricProxy} and runs its setup, then adds the Atlantis listeners.
     */
    @Override
    public void onInitialize() {
        INSTANCE = this;
        this.proxy = new FabricProxy();
        this.proxy.setup();
        Atlantis.listeners();
    }

    /**
     * @return the instance of the mod, or {@code null} before Fabric initializes it
     */
    public static AtlantisFabric instance() {
        return INSTANCE;
    }

    /**
     * @return the Fabric implementation of the mod loader delegate
     */
    public FabricProxy proxy() {
        return this.proxy;
    }
}
