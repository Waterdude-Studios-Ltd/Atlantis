package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.StorageTestConfigs;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static de.waterdu.atlantis.file.StorageTestConfigs.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Volatile} storage, which keeps data only in memory.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class VolatileTest {

    /**
     * Checks that writes and reads succeed, leave the data in memory alone and create no files.
     *
     * @throws InterruptedException if the wait for a storage operation is interrupted
     */
    @Test
    void writesAndReadsSucceedWithoutTouchingDisk() throws InterruptedException {
        var config = StorageTestConfigs.data(new Volatile());
        var storage = config.storage();
        var data = StorageTestConfigs.newData(1);
        config.put(data);

        assertThat(await(storage.write(config))).isTrue();
        assertThat(await(storage.writeSpecific(config, data))).isTrue();
        assertThat(await(storage.read(config, false))).isTrue();
        assertThat(await(storage.readFromUUID(config, data.uuid()))).isNull();
        assertThat(await(storage.readFromName(config, "missing"))).isNull();

        assertThat(config.all()).as("reads leave memory alone").containsExactly(data);
        assertThat(StorageTestConfigs.modDirFiles()).isEmpty();
    }

    /**
     * Checks that a delete returns the data and removes it from the config.
     */
    @Test
    void deleteRemovesFromMemory() {
        var config = StorageTestConfigs.data(new Volatile());
        var data = StorageTestConfigs.newData(1);
        config.put(data);

        assertThat(await(config.storage().delete(config, data))).isSameAs(data);

        assertThat(config.uuidDataMap()).isEmpty();
    }

    /**
     * Checks that a delete by an unknown UUID or name completes with {@code null}.
     */
    @Test
    void deletingNothingCompletesEmpty() {
        Config<?> config = StorageTestConfigs.data(new Volatile());

        assertThat(await(config.storage().deleteFromUUID(config, UUID.randomUUID()))).isNull();
        assertThat(await(config.storage().deleteFromName(config, "missing"))).isNull();
    }

    /**
     * Checks that a delete after the server has stopped completes with {@code null} and keeps the data.
     */
    @Test
    void deletesCompleteEmptyOnceTheServerHasStopped() {
        var config = StorageTestConfigs.data(new Volatile());
        var data = StorageTestConfigs.newData(1);
        config.put(data);
        Storage.SERVER_STARTED.set(true);

        assertThat(config.storage().isServerAlive()).isFalse();
        assertThat(await(config.storage().deleteFromUUID(config, data.uuid()))).isNull();
        assertThat(config.all()).containsExactly(data);
    }
}
