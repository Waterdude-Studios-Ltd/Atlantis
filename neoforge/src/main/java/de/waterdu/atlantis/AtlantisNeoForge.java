package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.neoforge.NeoForgeProxy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Atlantis.MOD_ID)
public class AtlantisNeoForge {

    public AtlantisNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        new NeoForgeProxy(modEventBus, modContainer);
        Atlantis.listeners();
    }
}
