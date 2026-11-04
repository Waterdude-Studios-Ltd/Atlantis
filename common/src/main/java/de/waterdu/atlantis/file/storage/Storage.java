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

/**
 * Reads, writes and deletes the instances of a {@link Config} in a backend, such as files, a database or Redis.
 */
public interface Storage {

    AtomicBoolean SERVER_STARTED = new AtomicBoolean(false);

    /**
     * Prepares the storage for a configuration. The config calls this once, before it reads or writes.
     *
     * @param config the config that uses this storage
     * @param modId the ID of the mod that owns the config
     * @param key the key of the config
     * @param path the path of the file or directory of the config, relative to the directory of the mod
     * @param gson the Gson instance of the config
     * @param configuration the default configuration instance
     * @param classes the classes of the config
     * @param <T> the configuration type
     * @throws InvalidClassException if the storage does not support the configuration type
     * @throws NoSuchFieldException if the configuration type does not have a field that the storage needs
     * @throws SQLException if the storage cannot connect to its database
     */
    <T extends Configuration> void init(Config<T> config, String modId, String key, String path, Gson gson, T configuration, ClassHolder<T> classes) throws InvalidClassException, NoSuchFieldException, SQLException;

    /**
     * Releases the resources of the storage, such as connections.
     *
     * @param config the config that uses this storage
     * @param <T> the configuration type
     */
    <T extends Configuration> void destruct(Config<T> config);

    /**
     * Writes all instances of the config that are in memory.
     *
     * @param config the config to write
     * @param <T> the configuration type
     * @return a future of {@code true} if the write was successful
     */
    <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config);

    /**
     * Writes one data instance of the config.
     *
     * @param config the config of the data
     * @param data the data instance to write
     * @param <T> the configuration type
     * @return a future of {@code true} if the write was successful
     * @throws InterruptedException if the thread is interrupted while the storage writes
     */
    <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) throws InterruptedException;

    /**
     * @param skipNulls keeps the current configuration if the stored value is {@code null}
     * @return a future of {@code false} if the storage did not read a value
     */
    <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls);

    /**
     * Reads all data instances of the config and puts them in memory.
     *
     * @param config the config to read
     * @param dir the directory to read from. Storages that do not use files ignore it.
     * @param names the file names of the instances that were in memory before the read
     * @param <T> the configuration type
     * @return a future of {@code true} if the read was successful
     */
    <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names);

    /**
     * Also puts the instance in memory.
     *
     * @return a future of the instance, or of {@code null} if there is none
     */
    <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid);

    /**
     * Reads the named data instance with the given name and puts it in memory.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none
     */
    <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name);

    /**
     * Deletes the data instance from memory and from the storage.
     *
     * @param config the config of the data
     * @param data the data instance to delete
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data);

    /**
     * Deletes the data instance with the given UUID from memory and from the storage.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid);

    /**
     * Deletes the named data instance with the given name from memory and from the storage.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name);

    /**
     * Reads the data instance of the player with {@link #readFromUUID}.
     *
     * @param config the config of the data
     * @param player the player whose UUID identifies the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none
     */
    default <T extends Configuration> CompletableFuture<T> readFromPlayer(Config<T> config, Player player) {
        return this.readFromUUID(config, player.getUUID());
    }

    /**
     * Deletes the data instance of the player with {@link #deleteFromUUID}.
     *
     * @param config the config of the data
     * @param player the player whose UUID identifies the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    default <T extends Configuration> CompletableFuture<T> deleteFromPlayer(Config<T> config, Player player) {
        return this.deleteFromUUID(config, player.getUUID());
    }

    /**
     * Gets the player data of the joining player, or creates and saves new data if there is none. Then it runs the
     * login callback of the config. The eviction setting can first remove old data of the player from memory.
     *
     * @param config the config of the player data
     * @param player the player that joins
     * @param <T> the configuration type
     */
    default <T extends Configuration> void onPlayerJoin(Config<T> config, ServerPlayer player) {
        if (Settings.get().getPlayerDataEvictionAlgorithm().passes(config)) {
            config.evict(player);
        }

        var clazz = config.classes().<T>playerDataClass();
        if (clazz == null) {
            return;
        }

        AtlantisConfigProxy.of(config.owner()).get(clazz, player.getUUID()).whenComplete((data, err) -> {
            if (data == null) {
                var constructor = config.classes().playerDataConstructor();
                if (constructor != null) {
                    try {
                        var newData = (PlayerData) constructor.newInstance(player);
                        AtlantisConfigProxy.of(config.owner()).put(clazz, newData);
                        config.classes().invokeLoginCallback(config, newData, player);
                        newData.save();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        AtlantisLogger.warn("No valid ServerPlayer constructor for data {}", config.name());
                    }
                }
            } else {
                config.classes().invokeLoginCallback(config, (PlayerData) data, player);
            }
        });
    }

    /**
     * Runs the logout callback of the config for the data of the leaving player. It writes the data if it waits for
     * an asynchronous save. The eviction setting can then remove the data from memory.
     *
     * @param config the config of the player data
     * @param player the player that leaves
     * @param <T> the configuration type
     */
    default <T extends Configuration> void onPlayerLeave(Config<T> config, ServerPlayer player) {
        var clazz = config.classes().<T>playerDataClass();
        if (clazz == null) {
            return;
        }

        AtlantisConfigProxy.of(config.owner()).get(clazz, player.getUUID()).whenComplete((data, err) -> {
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
        });
    }

    /**
     * Storages skip reads and writes while this is {@code false}.
     *
     * @return {@code true} before the server starts and while it runs. {@code false} after it stops.
     */
    default boolean isServerAlive() {
        boolean alive = ServerUtils.fromServer(server -> server.isRunning() && !server.isStopped(), false);
        var started = SERVER_STARTED.get();
        if (alive && !started) {
            SERVER_STARTED.set(true);
        }
        return alive || !started;
    }
}
