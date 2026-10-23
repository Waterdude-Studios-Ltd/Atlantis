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

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class SettingsTest {

    @Test
    void defaultsBeforeAtlantisConfigIsReady() {
        var settings = Settings.get();

        assertThat(Settings.get()).isSameAs(settings);
        assertThat(settings.getDataSaveDelay()).isEqualTo(30);
        assertThat(settings.getScheduledTaskDelay()).isEqualTo(500);
        assertThat(settings.getDuplicateSaveDelay()).isEqualTo(50);
        assertThat(settings.isUseErrorBin()).isFalse();
    }

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
