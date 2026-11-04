package de.waterdu.atlantis.test;

import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A {@link ModLoaderDelegate} for JUnit tests. It has no server unless a test provides one. Tests control the loaded
 * mods and the classes that the annotation scan reports. Block hooks allow everything.
 */
public final class TestModLoader implements ModLoaderDelegate {

    private static final ModObjectKey NODE_TYPE = new ModObjectKey("atlantis_test", "permission_node");

    private final Path configDir;
    private @Nullable MinecraftServer server;
    private final Set<String> loadedMods = new HashSet<>(Set.of("atlantis"));
    private final Map<Class<? extends Annotation>, List<String>> annotatedClasses = new HashMap<>();

    /**
     * Creates a test loader. Only {@code atlantis} is loaded at first.
     *
     * @param configDir the directory that {@link #configDir()} reports
     */
    TestModLoader(Path configDir) {
        this.configDir = configDir;
    }

    /**
     * Marks a mod as loaded, so that {@link #isModLoaded(String)} reports it.
     *
     * @param modId the ID of the mod to load
     * @return this loader
     */
    public TestModLoader loadMod(String modId) {
        this.loadedMods.add(modId);
        return this;
    }

    /**
     * Reports the names of the given classes from the annotation scan for the annotation.
     *
     * @param annotation the annotation that the scan looks for
     * @param classes the classes to report
     * @return this loader
     */
    public TestModLoader annotated(Class<? extends Annotation> annotation, Class<?>... classes) {
        var names = this.annotatedClasses.computeIfAbsent(annotation, __ -> new ArrayList<>());
        for (var clazz : classes) {
            names.add(clazz.getName());
        }
        return this;
    }

    /**
     * Reports a class name from the annotation scan without loading it, for example one that does not exist.
     */
    public TestModLoader annotatedName(Class<? extends Annotation> annotation, String className) {
        this.annotatedClasses.computeIfAbsent(annotation, __ -> new ArrayList<>()).add(className);
        return this;
    }

    /**
     * Gets the mod event bus, which is a no-op sink.
     *
     * @return {@link UEventBus.SinkEventBus#INSTANCE}
     */
    @Override
    public UEventBus<?, ?> modEventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    /**
     * Gets the game event bus, which is a no-op sink.
     *
     * @return {@link UEventBus.SinkEventBus#INSTANCE}
     */
    @Override
    public UEventBus<?, ?> eventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    /**
     * Reports the given server, usually a mock, from {@link #server()}.
     */
    public TestModLoader server(@Nullable MinecraftServer server) {
        this.server = server;
        return this;
    }

    /**
     * Gets the server that a test set with {@link #server(MinecraftServer)}.
     *
     * @return the server, or {@code null} if no test set one
     */
    @Override
    public @Nullable MinecraftServer server() {
        return this.server;
    }

    /**
     * @return the config directory given to the constructor
     */
    @Override
    public Path configDir() {
        return this.configDir;
    }

    /**
     * Checks if this is a dedicated server. The test loader always reports a dedicated server.
     *
     * @return {@code true}
     */
    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    /**
     * Checks if a mod is loaded. Only {@code atlantis} and the mods given to {@link #loadMod(String)} are loaded.
     *
     * @param modId the ID of the mod
     * @return {@code true} if the mod is loaded
     */
    @Override
    public boolean isModLoaded(String modId) {
        return this.loadedMods.contains(modId);
    }

    /**
     * Gets the loaded mods. The test loader reports none.
     *
     * @return an empty list
     */
    @Override
    public List<ModInfo> mods() {
        return List.of();
    }

    /**
     * Checks if a player is a fake player. The test loader reports no fake players.
     *
     * @param player the player to check
     * @return {@code false}
     */
    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return false;
    }

    /**
     * Gets the class names that tests registered with {@link #annotated} and {@link #annotatedName} for each
     * annotation.
     *
     * @param annotations the annotations to scan for
     * @return a map from each annotation to a copy of its class names, empty if none were registered
     */
    @Override
    public Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations) {
        var result = new HashMap<Class<? extends Annotation>, List<String>>();
        for (var annotation : annotations) {
            result.put(annotation, List.copyOf(this.annotatedClasses.getOrDefault(annotation, List.of())));
        }
        return result;
    }

    /**
     * Checks if a player can place a block. The test loader allows every placement.
     *
     * @param player the player who places the block
     * @param pos the position of the block
     * @return {@code true}
     */
    @Override
    public boolean onBlockPlace(ServerPlayer player, BlockPos pos) {
        return true;
    }

    /**
     * Checks if a player can break a block. The test loader allows every break.
     *
     * @param player the player who breaks the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true}
     */
    @Override
    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    /**
     * Does nothing after a player breaks a block.
     *
     * @param player the player who broke the block
     * @param pos the position of the block
     * @param state the state of the block
     * @param blockEntity the block entity of the block, or {@code null} if it had none
     */
    @Override
    public void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {}

    /**
     * Checks if a player can harvest a block. The test loader allows every harvest.
     *
     * @param player the player who harvests the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true}
     */
    @Override
    public boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    /**
     * Checks if an entity can destroy a block. The test loader allows every destruction.
     *
     * @param player the player who destroys the block
     * @param pos the position of the block
     * @param state the state of the block
     * @return {@code true}
     */
    @Override
    public boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    /**
     * Does nothing when a player destroys an item.
     *
     * @param player the player who destroyed the item
     * @param stack the destroyed item stack
     * @param hand the hand that held the item
     */
    @Override
    public void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand) {}

    /**
     * Creates a permission node with the key {@code namespace.path}. The node always uses the default resolver.
     *
     * @param namespace the namespace of the node
     * @param path the path of the node
     * @param defaultResolver gets the value of the node for a player
     * @return the permission node
     */
    @Override
    public UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver) {
        var key = namespace + "." + path;
        return new UPermissionNode<>(NODE_TYPE, key) {
            @Override
            public String key() {
                return key;
            }

            @Override
            public Boolean get(ServerPlayer player) {
                return defaultResolver.test(player);
            }
        };
    }
}
