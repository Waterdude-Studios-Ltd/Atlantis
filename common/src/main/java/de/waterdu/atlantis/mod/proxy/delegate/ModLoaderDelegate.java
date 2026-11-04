package de.waterdu.atlantis.mod.proxy.delegate;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigFinder;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.level.chunk.AtlantisChunkLoader;
import de.waterdu.atlantis.level.schematic.Schematics;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.server.ServerLifecycleEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.server.ServerTickEventPayload;
import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.wrapper.modloader.event.ListenerEvent;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.occasion.OccasionManager;
import de.waterdu.atlantis.playerdata.UUIDNameCache;
import de.waterdu.atlantis.self.AtlantisCommand;
import de.waterdu.atlantis.trident.TridentHost;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.TimeUtils;
import de.waterdu.atlantis.util.network.NetworkUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Gives common code access to everything that is specific to the mod loader. Get it with {@code Mods.modLoader()}.
 * The loader modules only call the hooks. The behavior is in the default methods.
 */
public interface ModLoaderDelegate extends ModProxy, TridentHost {

    /**
     * Runs {@link Bootstrapper#bootstrap} with this delegate.
     */
    @Override
    default void setup() {
        Bootstrapper.bootstrap(this);
    }

    /**
     * @param event the loader's setup event. On Fabric, this is {@code null}.
     * @param enqueue runs a task on the main thread after parallel mod setup. On Fabric, it runs the task at once.
     */
    default void init(Object event, Consumer<Runnable> enqueue) {
        AtlantisCommand.setup(enqueue);

        AtlantisUI.register();
        AtlantisData.register();

        Schematics.loadAll();

        OccasionManager.register();

        enqueue.accept(() -> {
            PageOptions.FormFactor.createDefaults(ServerUtils.registryAccess());
            Atlantis.CONFIG.config(Decorations.class).addAfterLoadTask(AtlantisData::mapDecorations);
        });

        AtlantisLogger.info("Welcome to Atlantis " + Atlantis.VERSION);
    }

    /**
     * Loads the configs, then fires {@link UEvent#SERVER_STARTING}.
     */
    default void onServerStarting() {
        AtlantisConfigFinder.init(true);
        this.event(UEvent.SERVER_STARTING).emit(new ServerLifecycleEventPayload.Starting(this.server()));
    }

    /**
     * Sets up the chunk loader, starts packet logging, then fires {@link UEvent#SERVER_STARTED}.
     */
    default void onServerStarted() {
        AtlantisChunkLoader.setup(Runnable::run);
        Atlantis.listeners().startLoggingPackets();
        this.event(UEvent.SERVER_STARTED).emit(new ServerLifecycleEventPayload.Started(this.server()));
    }

    /**
     * Fires {@link UEvent#SERVER_TICK_PRE} at the start of a server tick.
     *
     * @param server the server that ticks
     */
    default void onServerTickPre(MinecraftServer server) {
        this.event(UEvent.SERVER_TICK_PRE).emit(new ServerTickEventPayload.Pre(server));
    }

    /**
     * Advances the tick counters of {@link TimeUtils}, then fires {@link UEvent#SERVER_TICK_POST} at the end of a
     * server tick.
     *
     * @param server the server that ticks
     */
    default void onServerTickPost(MinecraftServer server) {
        TimeUtils.tick();
        this.event(UEvent.SERVER_TICK_POST).emit(new ServerTickEventPayload.Post(server));
    }

    /**
     * Fires {@link UEvent#SERVER_STOPPING}, then starts an asynchronous save of every config.
     */
    default void onServerStopping() {
        this.event(UEvent.SERVER_STOPPING).emit(new ServerLifecycleEventPayload.Stopping(this.server()));
        for (var config : AtlantisConfig.allInstances()) {
            config.asyncSaveInner();
        }
    }

    /**
     * Fires {@link UEvent#SERVER_STOPPED}.
     */
    default void onServerStopped() {
        this.event(UEvent.SERVER_STOPPED).emit(new ServerLifecycleEventPayload.Stopped(this.server()));
    }

    /**
     * Caches the name and address of the player, resets its {@link PlayerReference} and tells the player data storages
     * that it joined. Sends the update notice if Atlantis is outdated, then fires {@link UEvent#LOG_IN}.
     *
     * @param player the player that logged in
     */
    default void onPlayerLogin(ServerPlayer player) {
        UUIDNameCache.add(player);
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
        AtlantisConfig.onPlayerJoin(player);
        NetworkUtils.addAddress(player);
        AtlantisCommand.login(player);
        this.event(UEvent.LOG_IN).emit(new PlayerEventPayload.LogIn(player));
    }

    /**
     * Points the {@link PlayerReference} of the player to the new player entity.
     *
     * @param player the player entity after the respawn
     */
    default void onPlayerRespawn(ServerPlayer player) {
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
    }

    /**
     * Points the {@link PlayerReference} of the player to the player entity after the dimension change.
     *
     * @param player the player entity after the dimension change
     */
    default void onPlayerChangeDimension(ServerPlayer player) {
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
    }

    /**
     * Tells the player data storages that the player left, clears its {@link PlayerReference}, then fires
     * {@link UEvent#LOG_OUT}.
     *
     * @param player the player that logged out
     */
    default void onPlayerLogout(ServerPlayer player) {
        AtlantisConfig.onPlayerLeave(player);
        PlayerReference.getSafely(player).ifPresent(PlayerReference::reset);
        this.event(UEvent.LOG_OUT).emit(new PlayerEventPayload.LogOut(player));
    }

    /**
     * Gets the event for the key, to listen to it or to emit it.
     *
     * @param type the key of the event, from the constants in {@link UEvent}
     * @param <Payload> the type of the payload of the event
     * @return the event
     */
    default <Payload extends EventPayload> UEvent<Payload> event(Key<UEvent<Payload>> type) {
        return ListenerEvent.of(type);
    }

    /**
     * On Fabric, this is a sink that ignores all listeners.
     */
    UEventBus<?, ?> modEventBus();

    /**
     * On Fabric, this is a sink that ignores all listeners.
     */
    UEventBus<?, ?> eventBus();

    /**
     * @return the running server, or {@code null} if no server is running
     */
    @Nullable MinecraftServer server();

    /**
     * @return the config directory of the loader
     */
    @Override
    Path configDir();

    /**
     * Checks if the game runs as a dedicated server.
     *
     * @return {@code true} on a dedicated server, {@code false} on a client
     */
    boolean isDedicatedServer();

    /**
     * Checks if a mod or a Tine with the ID is loaded.
     *
     * @param modId the ID of the mod
     * @return {@code true} if the mod is loaded
     */
    @Override
    boolean isModLoaded(String modId);

    /**
     * @return the information of every loaded mod, including Tines
     */
    @Override
    List<ModInfo> mods();

    /**
     * Checks if the player is a fake player of the loader.
     *
     * @param player the player to check
     * @return {@code true} if the player is a fake player
     */
    boolean isFakePlayer(ServerPlayer player);

    /**
     * Finds annotated classes without loading them.
     *
     * @return the binary names of the annotated classes, keyed by annotation. Annotations with no match have no entry.
     */
    Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations);

    /**
     * Fires the loader's block place event. On Fabric, there is no event and this always returns {@code false}.
     *
     * @return {@code true} if a listener canceled the placement. Note that this is the opposite of
     *     {@link #onBlockBreak}.
     */
    boolean onBlockPlace(ServerPlayer player, BlockPos pos);

    /**
     * Fires the loader's block break event before the block is removed.
     *
     * @return {@code true} if the player can break the block, {@code false} if a listener canceled it
     */
    boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state);

    /**
     * Fires the loader's event after a block is broken. On NeoForge, this does nothing.
     *
     * @param state the state of the block before it was broken
     * @param blockEntity the block entity of the block before it was broken
     */
    void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);

    /**
     * On NeoForge, listeners of the harvest check event can change the result.
     *
     * @return {@code true} if the block drops items when the player breaks it
     */
    boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state);

    /**
     * Fires NeoForge's entity destroy block event. On Fabric, there is no event and this always returns {@code true}.
     *
     * @return {@code true} if the player can destroy the block
     */
    boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state);

    /**
     * Fires NeoForge's event for an item that broke during use. On Fabric, this does nothing.
     *
     * @param stack a copy of the stack from before it broke
     */
    void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand);

    /**
     * Creates a node with the key {@code namespace.path}.
     *
     * @param defaultResolver gives the value when no permission handler sets one. The player can be {@code null} for
     *     offline checks.
     */
    UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver);
}
