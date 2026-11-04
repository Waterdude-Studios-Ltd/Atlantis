package de.waterdu.atlantis.mod.impl.neoforge;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.event.NeoForgeEventBus;
import de.waterdu.atlantis.mod.impl.neoforge.modloader.permission.NeoForgePermissionNode;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionStatus;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.trident.Trident;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.server.CommandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Implements {@link ModLoaderDelegate} for NeoForge. It connects the NeoForge events to the delegate hooks.
 */
public class NeoForgeProxy implements ModLoaderDelegate {

    private static final String AUTHORS = "authors";
    private static final String ISSUE_TRACKER = "issueTrackerURL";

    private final NeoForgeEventBus modEventBus;
    private final NeoForgeEventBus neoForgeEventBus;

    private final Set<PermissionNode<?>> registeredNodes = new HashSet<>();

    /**
     * Creates the proxy with wrappers for the mod event bus and {@link NeoForge#EVENT_BUS}.
     *
     * @param modEventBus the event bus of the mod
     * @param modContainer the container of the mod, which is not used
     */
    public NeoForgeProxy(IEventBus modEventBus, ModContainer modContainer) {
        this.modEventBus = new NeoForgeEventBus(modEventBus);
        this.neoForgeEventBus = new NeoForgeEventBus(NeoForge.EVENT_BUS);
    }

    /**
     * Calls {@link #init} at {@link FMLCommonSetupEvent} and {@link #onServerStarting} at the dedicated server or
     * client setup event, then runs the bootstrap.
     */
    @Override
    public void setup() {
        var bus = this.modEventBus.dereference();

        bus.addListener(FMLCommonSetupEvent.class, it -> this.init(it, it::enqueueWork));
        bus.addListener(FMLDedicatedServerSetupEvent.class, __ -> this.onServerStarting());
        bus.addListener(FMLClientSetupEvent.class, __ -> this.onServerStarting());

        ModLoaderDelegate.super.setup();
    }

    /**
     * Adds the listeners for commands, player events, server lifecycle, server ticks and permissions to
     * {@link NeoForge#EVENT_BUS}, then runs the common init.
     *
     * @param event the {@link FMLCommonSetupEvent}
     * @param enqueue runs a task on the main thread after parallel mod setup
     */
    @Override
    public void init(Object event, Consumer<Runnable> enqueue) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, it -> CommandRegistry.registerAll(it.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, this.playerEventHandler(this::onPlayerLogin));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, this.playerEventHandler(this::onPlayerRespawn));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, this.playerEventHandler(this::onPlayerChangeDimension));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, this.playerEventHandler(this::onPlayerLogout));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, __ -> this.onServerStarted());
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, __ -> this.onServerStopping());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, __ -> this.onServerStopped());
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, it -> this.onServerTickPre(it.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, it -> this.onServerTickPost(it.getServer()));
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, it -> Permissions.buildNodes(node -> it.addNodes(((NeoForgePermissionNode<?>) node).dereference())));
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Handler.class, this::registerPermissionHandler);

        Atlantis.listeners();

        ModLoaderDelegate.super.init(event, enqueue);
    }

    /**
     * Creates a listener that passes the player of the event to the scope. Events of non-server players are ignored.
     *
     * @param scope the action for the server player
     * @param <T> the type of the player event
     * @return the listener
     */
    private <T extends PlayerEvent> Consumer<T> playerEventHandler(Consumer<ServerPlayer> scope) {
        return event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                scope.accept(player);
            }
        };
    }

    /**
     * Adds the Atlantis permission handler. The handler stores the registered nodes and gives the value of the default
     * resolver of each node.
     *
     * @param event the event to add the handler to
     */
    private void registerPermissionHandler(PermissionGatherEvent.Handler event) {
        event.addPermissionHandler(Permissions.ATLANTIS_PERMISSIONS_HANDLER, nodes -> {
            this.registeredNodes.addAll(nodes);
            return new IPermissionHandler() {
                @Override
                public @NonNull ResourceLocation getIdentifier() {
                    return Permissions.ATLANTIS_PERMISSIONS_HANDLER;
                }

                @Override
                public @NonNull Set<PermissionNode<?>> getRegisteredNodes() {
                    return NeoForgeProxy.this.registeredNodes;
                }

                @Override
                public <T> @NonNull T getPermission(@NonNull ServerPlayer serverPlayer, @NonNull PermissionNode<T> permissionNode, PermissionDynamicContext<?> @NonNull ... permissionDynamicContexts) {
                    return permissionNode.getDefaultResolver().resolve(serverPlayer, serverPlayer.getUUID(), permissionDynamicContexts);
                }

                @Override
                public <T> @NonNull T getOfflinePermission(@NonNull UUID uuid, @NonNull PermissionNode<T> permissionNode, PermissionDynamicContext<?> @NonNull ... permissionDynamicContexts) {
                    return permissionNode.getDefaultResolver().resolve(null, uuid, permissionDynamicContexts);
                }
            };
        });
    }

    /**
     * @return the wrapper for the event bus of the mod
     */
    @Override
    public UEventBus<?, ?> modEventBus() {
        return this.modEventBus;
    }

    /**
     * @return the wrapper for {@link NeoForge#EVENT_BUS}
     */
    @Override
    public UEventBus<?, ?> eventBus() {
        return this.neoForgeEventBus;
    }

    /**
     * @return the running server from {@link ServerLifecycleHooks}, or {@code null} if no server is running
     */
    @Override
    public @Nullable MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * @return the config directory from {@link FMLPaths#CONFIGDIR}
     */
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /**
     * Checks if the distribution is a dedicated server.
     *
     * @return {@code true} on a dedicated server, {@code false} on a client
     */
    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    /**
     * Checks if NeoForge has loaded the mod, or if Trident has loaded a Tine with the ID.
     *
     * @param modId the ID of the mod or Tine
     * @return {@code true} if the mod or Tine is loaded
     */
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId) || Trident.isLoaded(modId);
    }

    /**
     * Gets the info of the NeoForge mods and the loaded Tines. The authors and the issue tracker come from the mod
     * config.
     *
     * @return the info of the mods
     */
    @Override
    public List<ModInfo> mods() {
        var mods = new ArrayList<ModInfo>();
        for (var mod : ModList.get().getMods()) {
            var authors = mod.getConfig().getConfigElement(AUTHORS).map(String::valueOf).map(List::of).orElse(List.of());
            var tracker = mod.getOwningFile().getConfig().getConfigElement(ISSUE_TRACKER).map(String::valueOf).orElse(null);
            mods.add(new ModInfo(
                    mod.getModId(),
                    mod.getDisplayName(),
                    mod.getVersion().toString(),
                    authors,
                    tracker,
                    () -> NeoForgeProxy.versionCheck(mod)
            ));
        }
        mods.addAll(Trident.mods());
        return mods;
    }

    /**
     * Gets the result of the NeoForge version checker for the mod.
     *
     * @param mod the mod
     * @return the version check, with {@link VersionStatus#FAILED} if the checker has no result
     */
    private static VersionCheck versionCheck(IModInfo mod) {
        var result = VersionChecker.getResult(mod);
        if (result == null) {
            return new VersionCheck(VersionStatus.FAILED, null);
        }
        return new VersionCheck(VersionStatus.valueOf(result.status().name()), result.target() == null ? null : result.target().toString());
    }

    /**
     * Checks if the player is a NeoForge {@link FakePlayer}.
     *
     * @param player the player to check
     * @return {@code true} if the player is a fake player
     */
    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    /**
     * Finds the annotated classes in the scan data of {@link ModList}.
     *
     * @param annotations the annotations to look for
     * @return the binary names of the annotated classes, keyed by annotation. Annotations with no match have no entry.
     */
    @Override
    public Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations) {
        var types = new HashMap<Type, Class<? extends Annotation>>();
        for (var annotation : annotations) {
            types.put(Type.getType(annotation), annotation);
        }

        var classes = new HashMap<Class<? extends Annotation>, List<String>>();
        for (var scan : ModList.get().getAllScanData()) {
            for (var data : scan.getAnnotations()) {
                var annotation = types.get(data.annotationType());
                if (annotation != null) {
                    classes.computeIfAbsent(annotation, __ -> new ArrayList<>()).add(data.clazz().getClassName());
                }
            }
        }
        return classes;
    }

    /**
     * Installs a {@link TridentClassLoader} as a child of the class loader of Atlantis.
     *
     * @param jars the jars and directories of the Tines
     * @return the new class loader
     */
    @Override
    public ClassLoader addToClassPath(List<Path> jars) {
        return TridentClassLoader.install(jars, NeoForgeProxy.class.getClassLoader());
    }

    /**
     * Fires the NeoForge block place event for the block at the position, placed against the upward face.
     *
     * @param player the player that places the block
     * @param pos the position of the block
     * @return {@code true} if a listener canceled the placement
     */
    @Override
    public boolean onBlockPlace(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        return EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
    }

    /**
     * Posts a {@link BlockEvent.BreakEvent} on {@link NeoForge#EVENT_BUS}.
     *
     * @param player the player that breaks the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true} if the player can break the block, {@code false} if a listener canceled it
     */
    @Override
    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(player.serverLevel(), pos, state, player)).isCanceled();
    }

    /**
     * Does nothing on NeoForge.
     *
     * @param player the player that broke the block
     * @param pos the position of the block
     * @param state the state of the block before it was broken
     * @param blockEntity the block entity of the block before it was broken
     */
    @Override
    public void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        // TODO: Implement event via mixin
    }

    /**
     * Runs the NeoForge harvest check, whose listeners can change the result.
     *
     * @param player the player that breaks the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true} if the block drops items when the player breaks it
     */
    @Override
    public boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state) {
        return EventHooks.doPlayerHarvestCheck(player, state, player.serverLevel(), pos);
    }

    /**
     * Fires the NeoForge entity destroy block event.
     *
     * @param player the player that destroys the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true} if the player can destroy the block
     */
    @Override
    public boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        return EventHooks.onEntityDestroyBlock(player, pos, state);
    }

    /**
     * Fires the NeoForge event for an item that broke during use.
     *
     * @param player the player that used the item
     * @param stack a copy of the stack from before it broke
     * @param hand the hand that held the item
     */
    @Override
    public void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        EventHooks.onPlayerDestroyItem(player, stack, hand);
    }

    /**
     * Creates a boolean NeoForge {@link PermissionNode} with the key {@code namespace.path}.
     *
     * @param namespace the namespace of the node
     * @param path the path of the node
     * @param defaultResolver gives the value when no permission handler sets one. The player can be {@code null} for
     *     offline checks.
     * @return the wrapped node
     */
    @Override
    public UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver) {
        return new NeoForgePermissionNode<>(new PermissionNode<>(
                namespace,
                path,
                PermissionTypes.BOOLEAN,
                (player, uuid, context) -> defaultResolver.test(player)
        ));
    }
}
