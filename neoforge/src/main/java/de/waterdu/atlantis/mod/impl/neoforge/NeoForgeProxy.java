package de.waterdu.atlantis.mod.impl.neoforge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.event.NeoForgeEvent;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.event.NeoForgeEventBus;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NeoForgeProxy implements ModLoaderDelegate {

    private final NeoForgeEventBus modEventBus;
    private final NeoForgeEventBus neoForgeEventBus;

    private final Cache<Key<?>, UEvent<?>> events = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final List<LiteralArgumentBuilder<CommandSourceStack>> commands = new CopyOnWriteArrayList<>();

    public NeoForgeProxy(IEventBus modEventBus, ModContainer modContainer) {
        this.modEventBus = new NeoForgeEventBus(modEventBus);
        this.neoForgeEventBus = new NeoForgeEventBus(NeoForge.EVENT_BUS);
    }

    @Override
    public void setup() {
        var bus = this.modEventBus.dereference();

        bus.addListener(FMLCommonSetupEvent.class, it -> this.init(it, it::enqueueWork));
        bus.addListener(FMLDedicatedServerSetupEvent.class, __ -> this.onServerStarting());
        bus.addListener(FMLClientSetupEvent.class, __ -> this.onServerStarting());

        ModLoaderDelegate.super.setup();
    }

    @Override
    public void init(Object event, Consumer<Runnable> enqueue) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, it -> this.registerCommands(it.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, this.playerEventHandler(this::onPlayerLogin));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, this.playerEventHandler(this::onPlayerChangeDimension));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, this.playerEventHandler(this::onPlayerLogout));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, __ -> this.onServerStarted());
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, __ -> this.onServerStopping());

        Atlantis.listeners();

        ModLoaderDelegate.super.init(event, enqueue);
    }

    private <T extends PlayerEvent> Consumer<T> playerEventHandler(Consumer<ServerPlayer> scope) {
        return event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                scope.accept(player);
            }
        };
    }

    @Override
    public void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
        this.commands.add(command);
    }

    @Override
    public List<LiteralArgumentBuilder<CommandSourceStack>> commandsToRegister() {
        return this.commands;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Payload extends EventPayload> UEvent<Payload> event(Key<UEvent<Payload>> eventType, UEventBus<?, ?> eventBus) {
        return (UEvent<Payload>) this.events.get(eventType, __ -> new NeoForgeEvent<>(eventType, (NeoForgeEventBus) eventBus));
    }

    @Override
    public UEventBus<?, ?> modEventBus() {
        return this.modEventBus;
    }

    @Override
    public UEventBus<?, ?> eventBus() {
        return this.neoForgeEventBus;
    }

    @Override
    public @Nullable MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
