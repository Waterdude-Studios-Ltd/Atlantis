package de.waterdu.atlantis.mod.impl.fabric;

import de.waterdu.atlantis.mod.impl.fabric.modloader.permission.FabricPermissionNode;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.server.CommandRegistry;
import io.github.classgraph.ClassGraph;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Implements {@link ModLoaderDelegate} on Fabric. It connects the Fabric API events to the hooks of the delegate.
 */
public class FabricProxy extends FabricTridentHost implements ModLoaderDelegate {

    private volatile @Nullable MinecraftServer server;

    /**
     * Registers the command, server lifecycle, tick and player listeners, runs the default setup, then runs
     * {@code init} directly.
     */
    @Override
    public void setup() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandRegistry.registerAll(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(it -> {
            this.server = it;
            this.onServerStarting();
            Permissions.buildNodes(__ -> {});
        });
        ServerLifecycleEvents.SERVER_STARTED.register(__ -> this.onServerStarted());
        ServerLifecycleEvents.SERVER_STOPPING.register(__ -> this.onServerStopping());
        ServerLifecycleEvents.SERVER_STOPPED.register(__ -> {
            this.onServerStopped();
            this.server = null;
        });

        ServerTickEvents.START_SERVER_TICK.register(this::onServerTickPre);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTickPost);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> this.onPlayerLogin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.onPlayerLogout(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> this.onPlayerRespawn(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> this.onPlayerChangeDimension(player));

        ModLoaderDelegate.super.setup();

        this.init(null, Runnable::run);
    }

    /**
     * @return {@link UEventBus.SinkEventBus#INSTANCE}, because Fabric has no mod event bus
     */
    @Override
    public UEventBus<?, ?> modEventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    /**
     * @return {@link UEventBus.SinkEventBus#INSTANCE}, because Fabric has no event bus
     */
    @Override
    public UEventBus<?, ?> eventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    /**
     * @return the server that is set when it starts, or {@code null} before it starts and after it stops
     */
    @Override
    public @Nullable MinecraftServer server() {
        return this.server;
    }

    /**
     * Checks if the environment type of Fabric Loader is {@link EnvType#SERVER}.
     *
     * @return {@code true} on a dedicated server
     */
    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    /**
     * Checks if the player is a Fabric API {@link FakePlayer}.
     *
     * @param player the player to check
     * @return {@code true} if the player is a fake player
     */
    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    /**
     * Scans the paths of each mod that is not built in with ClassGraph, because a plain classpath scan does not see
     * mods that Knot loads. Minecraft, Mojang and Fabric packages are not scanned.
     *
     * @param annotations the annotations to find
     * @return the binary names of the annotated classes, keyed by annotation. Each annotation has an entry, which can
     *     be empty.
     */
    @Override
    public Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations) {
        var paths = new LinkedHashSet<Path>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            if (FabricTridentHost.isBuiltin(mod)) {
                continue;
            }
            for (var root : mod.getRootPaths()) {
                if (root.getFileSystem() == FileSystems.getDefault()) {
                    paths.add(root);
                }
            }
            if (mod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
                paths.addAll(mod.getOrigin().getPaths());
            }
        }

        var classes = new HashMap<Class<? extends Annotation>, List<String>>();
        try (var scan = new ClassGraph()
                .overrideClasspath(paths)
                .enableClassInfo()
                .enableAnnotationInfo()
                .ignoreClassVisibility()
                .rejectPackages("net.minecraft", "com.mojang", "net.fabricmc")
                .scan()) {
            for (var annotation : annotations) {
                classes.put(annotation, new ArrayList<>(scan.getClassesWithAnnotation(annotation.getName()).getNames()));
            }
        }
        return classes;
    }

    /**
     * Does nothing, because Fabric has no block place event.
     *
     * @param player the player that places the block
     * @param pos the position of the block
     * @return always {@code false}
     */
    @Override
    public boolean onBlockPlace(ServerPlayer player, BlockPos pos) {
        // TODO: Implement event via mixin
        return false;
    }

    /**
     * Fires {@link PlayerBlockBreakEvents#BEFORE} in the level of the player.
     *
     * @param player the player that breaks the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true} if the player can break the block, {@code false} if a listener canceled it
     */
    @Override
    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        var level = player.serverLevel();
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }

    /**
     * Fires {@link PlayerBlockBreakEvents#AFTER} in the level of the player.
     *
     * @param player the player that broke the block
     * @param pos the position of the block
     * @param state the state of the block before it was broken
     * @param blockEntity the block entity of the block before it was broken
     */
    @Override
    public void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(player.serverLevel(), player, pos, state, blockEntity);
    }

    /**
     * Checks if the player has the correct tool for drops of the block. No event is fired.
     *
     * @param player the player that breaks the block
     * @param pos the position of the block, which is not used
     * @param state the state of the block
     * @return {@code true} if the block drops items when the player breaks it
     */
    @Override
    public boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state) {
        return player.hasCorrectToolForDrops(state);
    }

    /**
     * Does nothing, because Fabric has no entity destroy block event.
     *
     * @param player the player that destroys the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return always {@code true}
     */
    @Override
    public boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        // TODO: Implement event via mixin
        return true;
    }

    /**
     * Does nothing, because Fabric has no event for an item that broke during use.
     *
     * @param player the player that used the item
     * @param stack a copy of the stack from before it broke
     * @param hand the hand that held the item
     */
    @Override
    public void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        // TODO: Implement event via mixin
    }

    /**
     * Creates a {@link FabricPermissionNode} with the key {@code namespace.path}.
     *
     * @param namespace the namespace of the node
     * @param path the path of the node
     * @param defaultResolver gives the value when no permission handler sets one. The player can be {@code null} for
     *     offline checks.
     * @return the new node
     */
    @Override
    public UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver) {
        return new FabricPermissionNode(new FabricPermissionNode.Node(namespace + "." + path, defaultResolver));
    }
}
