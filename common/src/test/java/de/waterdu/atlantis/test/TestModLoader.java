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
 * A {@link ModLoaderDelegate} for JUnit tests, with no server unless a test provides one. Tests control the loaded
 * mods and the classes the annotation scan reports. Block hooks allow everything.
 */
public final class TestModLoader implements ModLoaderDelegate {

    private static final ModObjectKey NODE_TYPE = new ModObjectKey("atlantis_test", "permission_node");

    private final Path configDir;
    private @Nullable MinecraftServer server;
    private final Set<String> loadedMods = new HashSet<>(Set.of("atlantis"));
    private final Map<Class<? extends Annotation>, List<String>> annotatedClasses = new HashMap<>();

    TestModLoader(Path configDir) {
        this.configDir = configDir;
    }

    public TestModLoader loadMod(String modId) {
        this.loadedMods.add(modId);
        return this;
    }

    public TestModLoader annotated(Class<? extends Annotation> annotation, Class<?>... classes) {
        var names = this.annotatedClasses.computeIfAbsent(annotation, __ -> new ArrayList<>());
        for (var clazz : classes) {
            names.add(clazz.getName());
        }
        return this;
    }

    /** Reports a class name from the annotation scan without loading it, e.g. one that does not exist. */
    public TestModLoader annotatedName(Class<? extends Annotation> annotation, String className) {
        this.annotatedClasses.computeIfAbsent(annotation, __ -> new ArrayList<>()).add(className);
        return this;
    }

    @Override
    public UEventBus<?, ?> modEventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    @Override
    public UEventBus<?, ?> eventBus() {
        return UEventBus.SinkEventBus.INSTANCE;
    }

    /** Reports the given server, typically a mock, from {@link #server()}. */
    public TestModLoader server(@Nullable MinecraftServer server) {
        this.server = server;
        return this;
    }

    @Override
    public @Nullable MinecraftServer server() {
        return this.server;
    }

    @Override
    public Path configDir() {
        return this.configDir;
    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return this.loadedMods.contains(modId);
    }

    @Override
    public List<ModInfo> mods() {
        return List.of();
    }

    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return false;
    }

    @Override
    public Map<Class<? extends Annotation>, List<String>> scanAnnotatedClasses(Collection<Class<? extends Annotation>> annotations) {
        var result = new HashMap<Class<? extends Annotation>, List<String>>();
        for (var annotation : annotations) {
            result.put(annotation, List.copyOf(this.annotatedClasses.getOrDefault(annotation, List.of())));
        }
        return result;
    }

    @Override
    public boolean onBlockPlace(ServerPlayer player, BlockPos pos) {
        return true;
    }

    @Override
    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void afterBlockBreak(ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {}

    @Override
    public boolean canHarvest(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean onEntityDestroyBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void onPlayerDestroyItem(ServerPlayer player, ItemStack stack, InteractionHand hand) {}

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
