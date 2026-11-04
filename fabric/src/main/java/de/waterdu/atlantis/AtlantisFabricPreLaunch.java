package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.fabric.FabricTridentHost;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Discovers Tines at the {@code preLaunch} entrypoint, before Minecraft classes load, so that Tine mixins can target
 * nearly any class.
 */
public class AtlantisFabricPreLaunch implements PreLaunchEntrypoint {

    /**
     * Runs {@link Bootstrapper#discoverTines} with a new {@link FabricTridentHost}.
     */
    @Override
    public void onPreLaunch() {
        Bootstrapper.discoverTines(new FabricTridentHost());
    }
}
