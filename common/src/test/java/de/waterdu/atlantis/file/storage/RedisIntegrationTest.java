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
 * Redis storage against a real Redis server. Two configs on the same channel stand in for two servers. Needs
 * Docker; skipped without it.
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

    @AfterEach
    void disconnect() {
        this.configs.forEach(config -> config.storage().destruct(config));
    }

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

    private static String channel() {
        return "channel_" + CHANNELS.incrementAndGet();
    }

    private <T extends RedisRecord> Config<T> register(Class<T> clazz, String channel) {
        return this.register(clazz, channel, new Redis("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379) + "/"));
    }

    private <T extends RedisRecord> Config<T> register(Class<T> clazz, String channel, Redis redis) {
        var config = StorageTestConfigs.register(clazz, channel, redis);
        assertThat(config).as("config registered").isNotNull();
        this.configs.add(config);
        return config;
    }
}
