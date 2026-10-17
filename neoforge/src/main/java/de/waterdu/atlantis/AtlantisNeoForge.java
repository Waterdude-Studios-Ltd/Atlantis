package de.waterdu.atlantis;

import de.waterdu.atlantis.mod.impl.neoforge.NeoForgeProxy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Atlantis.MOD_ID)
public class AtlantisNeoForge {

    private static AtlantisNeoForge INSTANCE;

    private final NeoForgeProxy proxy;

    public AtlantisNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        this.proxy = new NeoForgeProxy(modEventBus, modContainer);
        this.proxy.setup();
        Atlantis.listeners();
    }

    public static AtlantisNeoForge instance() {
        return INSTANCE;
    }

    public NeoForgeProxy proxy() {
        return this.proxy;
    }
}
