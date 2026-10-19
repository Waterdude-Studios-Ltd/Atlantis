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

public interface ModLoaderDelegate extends ModProxy {

    @Override
    default void setup() {
        Bootstrapper.bootstrap(this);
    }

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

    default void onServerStarting() {
        AtlantisConfigFinder.init(true);
    }

    default void onServerStarted() {
        AtlantisChunkLoader.setup(Runnable::run);
        Atlantis.listeners().startLoggingPackets();
    }

    default void onServerTickPre(MinecraftServer server) {
        this.event(UEvent.SERVER_TICK_PRE).emit(new ServerTickEventPayload.Pre(server));
    }

    default void onServerTickPost(MinecraftServer server) {
        TimeUtils.tick();
        this.event(UEvent.SERVER_TICK_POST).emit(new ServerTickEventPayload.Post(server));
    }

    default void onServerStopping() {
        for (var config : AtlantisConfig.allInstances()) {
            config.asyncSaveInner();
        }
    }

    default void onPlayerLogin(ServerPlayer player) {
        UUIDNameCache.add(player);
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
        AtlantisConfig.onPlayerJoin(player);
        NetworkUtils.addAddress(player);
        AtlantisCommand.login(player);
        this.event(UEvent.LOG_IN).emit(new PlayerEventPayload.LogIn(player));
    }

    default void onPlayerRespawn(ServerPlayer player) {
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
    }

    default void onPlayerChangeDimension(ServerPlayer player) {
        PlayerReference.getSafely(player).ifPresent(reference -> reference.reset(player));
    }

    default void onPlayerLogout(ServerPlayer player) {
        AtlantisConfig.onPlayerLeave(player);
        PlayerReference.getSafely(player).ifPresent(PlayerReference::reset);
        this.event(UEvent.LOG_OUT).emit(new PlayerEventPayload.LogOut(player));
    }

    default <Payload extends EventPayload> UEvent<Payload> event(Key<UEvent<Payload>> type) {
        return ListenerEvent.of(type);
    }

    UEventBus<?, ?> modEventBus();

    UEventBus<?, ?> eventBus();

    @Nullable MinecraftServer server();

    Path configDir();

    boolean isDedicatedServer();

    boolean isModLoaded(String modId);

    List<ModInfo> mods();

    boolean isFakePlayer(ServerPlayer player);

    Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations);

    boolean onBlockPlace(ServerPlayer player, BlockPos pos);

    boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state);

    void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);

    boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state);

    boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state);

    void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand);

    UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver);
}
