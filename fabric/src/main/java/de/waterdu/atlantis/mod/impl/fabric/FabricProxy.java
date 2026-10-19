package de.waterdu.atlantis.mod.impl.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class FabricProxy implements ModLoaderDelegate {

    private volatile @Nullable MinecraftServer server;

    @Override
    public void setup() {
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
    @SuppressWarnings("unchecked")
    public void init(Object event, Consumer<Runnable> enqueue) {
        try {
            Class<?> callbackClass = Class.forName("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback");
            Object callbackEvent = callbackClass.getField("EVENT").get(null);
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("register") || method.getName().equals("registerCommand")) {
                            if (args != null && args.length > 0 && args[0] != null) {
                                this.registerCommands((com.mojang.brigadier.CommandDispatcher<CommandSourceStack>) args[0]);
                            }
                        }
                        return null;
                    }
            );
            callbackEvent.getClass().getMethod("register", callbackClass).invoke(callbackEvent, callback);
        } catch (Throwable ignored) {
        }

        ModLoaderDelegate.super.init(event, enqueue);
    }

    @Override
    public void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command) {

    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> commandsToRegister() {
        return List.of();
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
