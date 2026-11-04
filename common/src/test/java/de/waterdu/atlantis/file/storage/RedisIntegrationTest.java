package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.StorageTestConfigs;
import de.waterdu.atlantis.file.storage.DatabaseFixtures.RedisRecord;
import de.waterdu.atlantis.file.storage.DatabaseFixtures.RedisReplica;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.config.RedisSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.waterdu.atlantis.file.StorageTestConfigs.await;
import static de.waterdu.atlantis.file.StorageTestConfigs.awaitUntil;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis storage against a real Redis server. Two configs on the same channel represent two servers. Needs Docker.
 * Without Docker, the test is skipped.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class RedisIntegrationTest {

    private static final AtomicInteger CHANNELS = new AtomicInteger();

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private final List<Config<?>> configs = new ArrayList<>();

    /**
     * Destructs the storage of every config that the test registered, which disconnects it from Redis.
     */
    @AfterEach
    void disconnect() {
        this.configs.forEach(config -> config.storage().destruct(config));
    }

    /**
     * Checks that a record written by one server reaches the other server with its values.
     *
     * @throws Exception if a storage operation fails
     */
    @Test
    void writesReachOtherServers() throws Exception {
        var channel = channel();
        var first = this.register(RedisRecord.class, channel);
        var second = this.register(RedisReplica.class, channel);
        var record = new RedisRecord();
        record.value = 7;
        first.put(record);

        assertThat(await(first.storage().writeSpecific(first, record))).isTrue();

        awaitUntil("the second server receives the record", () -> second.uuidDataMap().containsKey(record.uuid()));
        assertThat(((RedisRecord) second.uuidDataMap().get(record.uuid())).value).isEqualTo(7);
    }

    /**
     * Checks that a record deleted by one server is also removed from the other server.
     *
     * @throws Exception if a storage operation fails
     */
    @Test
    void deletesReachOtherServers() throws Exception {
        var channel = channel();
        var first = this.register(RedisRecord.class, channel);
        var second = this.register(RedisReplica.class, channel);
        var record = new RedisRecord();
        first.put(record);
        await(first.storage().writeSpecific(first, record));
        awaitUntil("the second server receives the record", () -> second.uuidDataMap().containsKey(record.uuid()));

        assertThat(await(first.storage().delete(first, record))).isSameAs(record);

        awaitUntil("the second server deletes the record", () -> second.uuidDataMap().isEmpty());
    }

    /**
     * Checks that a server that joins the channel later receives the records of the first server.
     *
     * @throws Exception if a storage operation fails
     */
    @Test
    void joiningServerIsSynced() throws Exception {
        var channel = channel();
        var first = this.register(RedisRecord.class, channel);
        var record = new RedisRecord();
        record.value = 3;
        first.put(record);

        var second = this.register(RedisReplica.class, channel);

        awaitUntil("the joining server is synced", () -> second.uuidDataMap().containsKey(record.uuid()));
    }

    /**
     * Checks that storage from URL settings and from explicit settings registers configs and can publish.
     *
     * @throws Exception if a storage operation fails
     */
    @Test
    void settingsConnect() throws Exception {
        var url = new RedisSettings.URL("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379) + "/").storage("mod");
        var explicit = new RedisSettings.Explicit(REDIS.getHost(), REDIS.getMappedPort(6379), "", "").storage("mod");
        var urlConfig = this.register(RedisRecord.class, channel(), url);
        var explicitConfig = this.register(RedisReplica.class, channel(), explicit);

        url.publish("ping");
        explicit.publishAsync("ping");

        assertThat(urlConfig).isNotNull();
        assertThat(explicitConfig).isNotNull();
    }

    /**
     * @return a new channel name that no other test uses
     */
    private static String channel() {
        return "channel_" + CHANNELS.incrementAndGet();
    }

    /**
     * Registers a config with new Redis storage that connects to the Redis container.
     *
     * @param clazz the data class
     * @param channel the path of the config, used in the name of its Redis channel
     * @param <T> the type of the data
     * @return the registered config
     */
    private <T extends RedisRecord> Config<T> register(Class<T> clazz, String channel) {
        return this.register(clazz, channel, new Redis("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379) + "/"));
    }

    /**
     * Registers a config with the given Redis storage, and tracks it so that it disconnects after the test. Fails the
     * test if the config does not register.
     *
     * @param clazz the data class
     * @param channel the path of the config, used in the name of its Redis channel
     * @param redis the Redis storage to use
     * @param <T> the type of the data
     * @return the registered config
     */
    private <T extends RedisRecord> Config<T> register(Class<T> clazz, String channel, Redis redis) {
        var config = StorageTestConfigs.register(clazz, channel, redis);
        assertThat(config).as("config registered").isNotNull();
        this.configs.add(config);
        return config;
    }
}
