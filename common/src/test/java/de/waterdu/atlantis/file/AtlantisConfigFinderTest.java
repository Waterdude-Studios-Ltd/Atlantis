package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.ConfigFixtures.FallbackStorageConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.FirstConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.LastConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.NotAConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.SampleAon;
import de.waterdu.atlantis.file.ConfigFixtures.SampleConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.SampleData;
import de.waterdu.atlantis.file.ConfigFixtures.SampleLang;
import de.waterdu.atlantis.file.ConfigFixtures.VolatileConfiguration;
import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Volatile;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.test.TestModLoader;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static de.waterdu.atlantis.file.ConfigTestSupport.awaitContent;
import static de.waterdu.atlantis.file.ConfigTestSupport.awaitUntil;
import static de.waterdu.atlantis.file.ConfigTestSupport.modDir;
import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisConfigFinderTest {

    @Test
    void registersOneConfigPerAnnotation(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class)
                .annotated(BasicData.class, SampleData.class)
                .annotated(I18nFile.class, SampleLang.class)
                .annotated(FileBackedObject.class, SampleAon.class);

        findAndInit(loader);

        var instance = AtlantisConfig.of(ConfigFixtures.MOD);
        assertThat(instance.configs.keySet()).containsExactlyInAnyOrder(
                SampleConfiguration.class.getCanonicalName(),
                SampleData.class.getCanonicalName(),
                SampleLang.class.getCanonicalName(),
                SampleAon.class.getCanonicalName()
        );
        assertThat(instance.ready()).isTrue();
        assertThat(instance.config(SampleLang.class.getCanonicalName()).isLang()).isTrue();
        assertThat(instance.config(SampleAon.class.getCanonicalName()).isAON()).isTrue();
        assertThat(instance.config(SampleData.class.getCanonicalName()).shouldForceLoad()).isTrue();
    }

    @Test
    void filesAreCreatedInTheOwningModsDirectory(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class)
                .annotated(BasicData.class, SampleData.class)
                .annotated(I18nFile.class, SampleLang.class)
                .annotated(FileBackedObject.class, SampleAon.class);

        findAndInit(loader);

        var dir = loader.configDir().resolve(ConfigFixtures.MOD);
        assertThat(modDir()).isEqualTo(dir);
        assertThat(awaitContent(dir.resolve("sampleconfiguration.json"), "\"text\"")).contains("\"number\": 1", "\"text\": \"default\"");
        assertThat(awaitContent(dir.resolve("lang.json"), "\"greeting\"")).contains("Hello %1");
        assertThat(awaitContent(dir.resolve("commented.aon"), "count")).contains("# Sample AON file", "# How many things");
        assertThat(dir.resolve("sampledata")).isDirectory();
    }

    @Test
    void ownerModIsTheAnnotationsMod(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.findOwnerMod(SampleConfiguration.class)).isEqualTo(ConfigFixtures.MOD);
        assertThat(new SampleConfiguration().proxy()).isSameAs(AtlantisConfigProxy.of(ConfigFixtures.MOD));
    }

    @Test
    void registersInLoadOrder(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, LastConfiguration.class, SampleAon.class, FirstConfiguration.class);
        var registered = recordRegistrations();

        findAndInit(loader);

        assertThat(registered).containsExactly(FirstConfiguration.class, SampleAon.class, LastConfiguration.class);
    }

    @Test
    void registerListenerCanChangeThePath(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class);
        Mods.modLoader().event(UEvent.REGISTER_CONFIG).listen(payload -> {
            payload.setPath("renamed.json");
            return InteractionResult.PASS;
        });

        findAndInit(loader);

        awaitUntil("renamed.json is written", () -> ConfigTestSupport.read(modDir().resolve("renamed.json")).contains("\"number\""));
        assertThat(modDir().resolve("sampleconfiguration.json")).doesNotExist();
    }

    @Test
    void skipsAnnotatedClassesThatAreNotConfigurations(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, NotAConfiguration.class, SampleConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.of(ConfigFixtures.MOD).configs.keySet()).containsExactly(SampleConfiguration.class.getCanonicalName());
    }

    @Test
    void usesTheAnnotatedStorage(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, VolatileConfiguration.class, SampleAon.class);

        findAndInit(loader);

        var instance = AtlantisConfig.of(ConfigFixtures.MOD);
        assertThat(instance.config(VolatileConfiguration.class.getCanonicalName()).storage()).isInstanceOf(Volatile.class);
        assertThat(instance.config(SampleAon.class.getCanonicalName()).storage()).isInstanceOf(Flatfile.class);
    }

    @Test
    void fallsBackToFlatfileWhenTheStorageCannotBeCreated(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, FallbackStorageConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.of(ConfigFixtures.MOD).config(FallbackStorageConfiguration.class.getCanonicalName()).storage()).isInstanceOf(Flatfile.class);
    }

    @Test
    void findOnlyScansOnce(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class);
        var registered = recordRegistrations();
        Bootstrapper.bootstrap(loader);

        AtlantisConfigFinder.find();
        AtlantisConfigFinder.find();
        AtlantisConfigFinder.init(true);

        assertThat(registered).containsExactly(SampleConfiguration.class);
    }

    @Test
    void restoreFromBackupAllowsRegisteringAgain(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class);
        var registered = recordRegistrations();
        findAndInit(loader);

        AtlantisConfigFinder.init(true);
        assertThat(registered).as("init consumes the found configs").hasSize(1);

        AtlantisConfigFinder.restoreFromBackup();
        AtlantisConfigFinder.init(true);
        assertThat(registered).containsExactly(SampleConfiguration.class, SampleConfiguration.class);
    }

    private static void findAndInit(TestModLoader loader) {
        Bootstrapper.bootstrap(loader);
        AtlantisConfigFinder.find();
        AtlantisConfigFinder.init(true);
    }

    private static List<Class<?>> recordRegistrations() {
        var registered = new ArrayList<Class<?>>();
        Mods.modLoader().event(UEvent.REGISTER_CONFIG).listen(payload -> {
            registered.add(payload.classes().getConfigurationClass());
            return InteractionResult.PASS;
        });
        return registered;
    }
}
