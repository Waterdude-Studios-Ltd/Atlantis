package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.fabric.FabricProxy;
import net.fabricmc.api.ModInitializer;

public class AtlantisFabric implements ModInitializer {

    public static AtlantisFabric INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        Mods
        new FabricProxy().onInitialize();
        Atlantis.listeners();
    }
}
