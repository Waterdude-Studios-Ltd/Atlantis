package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.fabric.FabricProxy;
import net.fabricmc.api.ModInitializer;

public class AtlantisFabric implements ModInitializer {

    public static AtlantisFabric INSTANCE;

    private FabricProxy proxy;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        this.proxy = new FabricProxy();
        this.proxy.setup();
        Atlantis.listeners();
    }

    public static AtlantisFabric instance() {
        return INSTANCE;
    }

    public FabricProxy proxy() {
        return this.proxy;
    }
}
