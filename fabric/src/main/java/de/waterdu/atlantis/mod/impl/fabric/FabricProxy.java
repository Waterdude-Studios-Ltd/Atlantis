package de.waterdu.atlantis.mod.impl.fabric;

import de.waterdu.atlantis.mod.impl.fabric.modloader.permission.FabricPermissionNode;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEventBus;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import de.waterdu.atlantis.mod.wrapper.modloader.permission.UPermissionNode;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.util.server.CommandRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FabricProxy implements ModLoaderDelegate {

    private static final String BUILTIN_MOD_TYPE = "builtin";
    private static final String ISSUES_CONTACT = "issues";
    private static final String FABRIC_API_MODULE = "fabric-api:module-lifecycle";

    private volatile @Nullable MinecraftServer server;

    @Override
    public void setup() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandRegistry.registerAll(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(it -> {
            this.server = it;
            this.init(it, it::execute);
            this.onServerStarting();
            Permissions.buildNodes(__ -> {});
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

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<ModInfo> mods() {
        var mods = new ArrayList<ModInfo>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            if (FabricProxy.isBuiltin(mod) || mod.getContainingMod().isPresent() || mod.getMetadata().containsCustomValue(FABRIC_API_MODULE)) {
                continue;
            }
            var metadata = mod.getMetadata();
            mods.add(new ModInfo(
                    metadata.getId(),
                    metadata.getName(),
                    metadata.getVersion().getFriendlyString(),
                    metadata.getAuthors().stream().map(Person::getName).toList(),
                    metadata.getContact().get(ISSUES_CONTACT).orElse(null),
                    () -> VersionCheck.UNSUPPORTED
            ));
        }
        return mods;
    }

    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    private static boolean isBuiltin(ModContainer mod) {
        return BUILTIN_MOD_TYPE.equals(mod.getMetadata().getType());
    }

    @Override
    public UPermissionNode<?, Boolean> createBooleanNode(String namespace, String path, Predicate<@Nullable ServerPlayer> defaultResolver) {
        return new FabricPermissionNode(new FabricPermissionNode.Node(namespace + "." + path, defaultResolver));
    }
}
