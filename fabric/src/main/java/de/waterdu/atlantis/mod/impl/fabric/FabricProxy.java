package de.waterdu.atlantis.mod.impl.fabric;

import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.util.server.CommandRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public class FabricProxy implements ModLoaderDelegate {

    private volatile @Nullable MinecraftServer server;

    @Override
    public void setup() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandRegistry.registerAll(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(it -> {
            this.server = it;
            this.init(it, it::execute);
            this.onServerStarting();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(__ -> this.onServerStarted());
        ServerLifecycleEvents.SERVER_STOPPING.register(__ -> this.onServerStopping());
        ServerLifecycleEvents.SERVER_STOPPED.register(__ -> this.server = null);

        ModLoaderDelegate.super.setup();
    }

    @Override
    public UEventBus<?, ?> modEventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    @Override
    public UEventBus<?, ?> eventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    @Override
    public @Nullable MinecraftServer server() {
        return this.server;
    }
}
