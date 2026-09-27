package de.waterdu.atlantis.file.storage;

import com.google.gson.Gson;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Storage {

    AtomicBoolean SERVER_STARTED = new AtomicBoolean(false);

    <T extends Configuration> void init(Config<T> config, String modID, String key, String path, Gson gson, T configuration, ClassHolder<T> classes) throws InvalidClassException, NoSuchFieldException, SQLException;

    <T extends Configuration> void destruct(Config<T> config);

    <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config);

    <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) throws InterruptedException;

    <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls);

    <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names);

    <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid);

    <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name);

    <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data);

    <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid);

    <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name);

    default <T extends Configuration> CompletableFuture<T> readFromPlayer(Config<T> config, Player player) {
        return this.readFromUUID(config, player.getUUID());
    }

    default <T extends Configuration> CompletableFuture<T> deleteFromPlayer(Config<T> config, Player player) {
        return this.deleteFromUUID(config, player.getUUID());
    }

    default <T extends Configuration> void onPlayerJoin(Config<T> config, ServerPlayer player) {
        if (Settings.get().getPlayerDataEvictionAlgorithm().passes(config)) {
            config.evict(player);
        }
        var clazzOpt = config.classes().<T>playerDataClass();
        clazzOpt.ifPresent(clazz -> AtlantisConfigProxy.of(config.owner()).get(clazz, player.getUUID()).whenComplete((data, err) -> {
            if (data == null) {
                config.classes().playerDataConstructor().ifPresent(constructor -> {
                    try {
                        var newData = (PlayerData) constructor.newInstance(player);
                        AtlantisConfigProxy.of(config.owner()).put(clazz, newData);
                        config.classes().invokeLoginCallback(config, newData, player);
                        newData.save();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        AtlantisLogger.warn("No valid ServerPlayer constructor for data {}", config.name());
                    }
                });
            } else {
                config.classes().invokeLoginCallback(config, (PlayerData) data, player);
            }
        }));
    }

    default <T extends Configuration> void onPlayerLeave(Config<T> config, ServerPlayer player) {
        var clazzOpt = config.classes().<T>playerDataClass();
        clazzOpt.ifPresent(clazz -> AtlantisConfigProxy.of(config.owner()).get(clazz, player.getUUID()).whenComplete((data, err) -> {
            if (data != null) {
                config.classes().invokeLogoutCallback(config, (PlayerData) data, player);
                if (config.markSaved(data.uuid())) {
                    try {
                        this.writeSpecific(config, data).whenComplete((result, err2) -> {
                            if (Settings.get().getPlayerDataEvictionAlgorithm().passes(config)) {
                                config.evict(data);
                            }
                        });
                    } catch (InterruptedException e) {
                        AtlantisLogger.error("Failed to save instance of {} called {} with UUID {}: {}", config.name(), data.identifier(), data.uuid(), e.getMessage());
                    }
                } else {
                    if (Settings.get().getPlayerDataEvictionAlgorithm().passes(config)) {
                        config.evict(data);
                    }
                }
            }
        }));
    }

    default boolean isServerAlive() {
        boolean alive = ServerUtils.fromServer(server -> server.isRunning() && !server.isStopped(), false);
        var started = SERVER_STARTED.get();
        if (alive && !started) {
            SERVER_STARTED.set(true);
        }
        return alive || !started;
    }
}
