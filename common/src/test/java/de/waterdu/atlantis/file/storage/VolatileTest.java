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

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class VolatileTest {

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

    @Test
    void deleteRemovesFromMemory() {
        var config = StorageTestConfigs.data(new Volatile());
        var data = StorageTestConfigs.newData(1);
        config.put(data);

        assertThat(await(config.storage().delete(config, data))).isSameAs(data);

        assertThat(config.uuidDataMap()).isEmpty();
    }

    @Test
    void deletingNothingCompletesEmpty() {
        Config<?> config = StorageTestConfigs.data(new Volatile());

        assertThat(await(config.storage().deleteFromUUID(config, UUID.randomUUID()))).isNull();
        assertThat(await(config.storage().deleteFromName(config, "missing"))).isNull();
    }

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
