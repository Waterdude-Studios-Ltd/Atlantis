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

/**
 * Tests {@link AtlantisConfigFinder}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisConfigFinderTest {

    /**
     * Checks that each config annotation registers one config of the matching kind.
     *
     * @param loader the test mod loader
     */
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

    /**
     * Checks that the files of each config are written in the directory of the mod that owns them.
     *
     * @param loader the test mod loader
     */
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

    /**
     * Checks that the owner mod of a config is the mod in its annotation, and that the config uses that mod's proxy.
     *
     * @param loader the test mod loader
     */
    @Test
    void ownerModIsTheAnnotationsMod(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, SampleConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.findOwnerMod(SampleConfiguration.class)).isEqualTo(ConfigFixtures.MOD);
        assertThat(new SampleConfiguration().proxy()).isSameAs(AtlantisConfigProxy.of(ConfigFixtures.MOD));
    }

    /**
     * Checks that configs register in their load order, not in the order of the scan.
     *
     * @param loader the test mod loader
     */
    @Test
    void registersInLoadOrder(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, LastConfiguration.class, SampleAon.class, FirstConfiguration.class);
        var registered = recordRegistrations();

        findAndInit(loader);

        assertThat(registered).containsExactly(FirstConfiguration.class, SampleAon.class, LastConfiguration.class);
    }

    /**
     * Checks that a {@link UEvent#REGISTER_CONFIG} listener can change the file that a config writes to.
     *
     * @param loader the test mod loader
     */
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

    /**
     * Checks that an annotated class that is not a configuration is not registered.
     *
     * @param loader the test mod loader
     */
    @Test
    void skipsAnnotatedClassesThatAreNotConfigurations(TestModLoader loader) {
        loader.annotated(BasicConfiguration.class, NotAConfiguration.class, SampleConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.of(ConfigFixtures.MOD).configs.keySet()).containsExactly(SampleConfiguration.class.getCanonicalName());
    }

    /**
     * Checks that a config uses the storage in its annotation, and flatfile storage when none is set.
     *
     * @param loader the test mod loader
     */
    @Test
    void usesTheAnnotatedStorage(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, VolatileConfiguration.class, SampleAon.class);

        findAndInit(loader);

        var instance = AtlantisConfig.of(ConfigFixtures.MOD);
        assertThat(instance.config(VolatileConfiguration.class.getCanonicalName()).storage()).isInstanceOf(Volatile.class);
        assertThat(instance.config(SampleAon.class.getCanonicalName()).storage()).isInstanceOf(Flatfile.class);
    }

    /**
     * Checks that a config uses flatfile storage when its annotated storage cannot be created.
     *
     * @param loader the test mod loader
     */
    @Test
    void fallsBackToFlatfileWhenTheStorageCannotBeCreated(TestModLoader loader) {
        loader.annotated(FileBackedObject.class, FallbackStorageConfiguration.class);

        findAndInit(loader);

        assertThat(AtlantisConfig.of(ConfigFixtures.MOD).config(FallbackStorageConfiguration.class.getCanonicalName()).storage()).isInstanceOf(Flatfile.class);
    }

    /**
     * Checks that a second call to {@link AtlantisConfigFinder#find()} does not register the configs again.
     *
     * @param loader the test mod loader
     */
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

    /**
     * Checks that a second init registers nothing, but that {@link AtlantisConfigFinder#restoreFromBackup()} lets the
     * next init register the configs again.
     *
     * @param loader the test mod loader
     */
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

    /**
     * Bootstraps the loader, then finds and initializes the configs.
     *
     * @param loader the test mod loader
     */
    private static void findAndInit(TestModLoader loader) {
        Bootstrapper.bootstrap(loader);
        AtlantisConfigFinder.find();
        AtlantisConfigFinder.init(true);
    }

    /**
     * Listens for {@link UEvent#REGISTER_CONFIG} and records the configuration class of each registration.
     *
     * @return a list that fills as configs register
     */
    private static List<Class<?>> recordRegistrations() {
        var registered = new ArrayList<Class<?>>();
        Mods.modLoader().event(UEvent.REGISTER_CONFIG).listen(payload -> {
            registered.add(payload.classes().getConfigurationClass());
            return InteractionResult.PASS;
        });
        return registered;
    }
}
