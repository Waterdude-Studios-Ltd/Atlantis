package de.waterdu.atlantis;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigFinder;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.test.TestModLoader;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Settings}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class SettingsTest {

    /**
     * Checks that {@link Settings#get()} gives the same default settings each time before the Atlantis config is
     * ready.
     */
    @Test
    void defaultsBeforeAtlantisConfigIsReady() {
        var settings = Settings.get();

        assertThat(Settings.get()).isSameAs(settings);
        assertThat(settings.getDataSaveDelay()).isEqualTo(30);
        assertThat(settings.getScheduledTaskDelay()).isEqualTo(500);
        assertThat(settings.getDuplicateSaveDelay()).isEqualTo(50);
        assertThat(settings.isUseErrorBin()).isFalse();
    }

    /**
     * Checks that {@link Settings#get()} gives the loaded settings after the configs initialize, and that the
     * settings file is written with its comments.
     *
     * @param loader the test mod loader
     * @throws Exception if the file cannot be read or the wait is interrupted
     */
    @Test
    void loadedSettingsOnceAtlantisConfigIsReady(TestModLoader loader) throws Exception {
        var defaults = Settings.get();
        loader.annotated(FileBackedObject.class, Settings.class);
        Bootstrapper.bootstrap(loader);
        AtlantisConfigFinder.find();

        AtlantisConfigFinder.init(false);

        assertThat(Settings.get()).isNotSameAs(defaults);
        var file = AtlantisConfig.dirOf(Atlantis.ID).toPath().resolve("atlantis.aon");
        var deadline = System.currentTimeMillis() + 10_000;
        while (!(Files.exists(file) && Files.readString(file).contains("Atlantis global settings")) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertThat(Files.readString(file)).contains("# Atlantis global settings");
    }
}
