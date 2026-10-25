package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.fabric.FabricTridentHost;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AtlantisFabricPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        Bootstrapper.discoverTines(new FabricTridentHost());
    }
}
