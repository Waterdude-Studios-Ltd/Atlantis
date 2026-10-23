package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.ConfigFixtures.SampleConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.SampleData;
import de.waterdu.atlantis.file.ConfigFixtures.SampleLang;
import de.waterdu.atlantis.file.ConfigFixtures.SampleNamedData;
import de.waterdu.atlantis.file.ConfigFixtures.SamplePlayerData;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Volatile;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.waterdu.atlantis.file.ConfigTestSupport.await;
import static de.waterdu.atlantis.file.ConfigTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class ConfigTest {

    @Test
    void describesItself() {
        var config = register(SampleConfiguration.class, new Volatile());

        assertThat(config.owner()).isEqualTo(ConfigFixtures.MOD);
        assertThat(config.key()).isEqualTo(SampleConfiguration.class.getCanonicalName());
        assertThat(config.name()).isEqualTo(ConfigFixtures.MOD + ":" + SampleConfiguration.class.getCanonicalName());
        assertThat(config.get()).isInstanceOf(SampleConfiguration.class);
        assertThat(config.isData()).isFalse();
        assertThat(config.isPlayerData()).isFalse();
        assertThat(config.dataFileExtension()).isEqualTo(FileExtensions.JSON);
    }

    @Test
    void aonConfigsUseTheAonExtension() {
        var config = register(SampleConfiguration.class, "sample.aon", new Volatile(), false, true);

        assertThat(config.isAON()).isTrue();
        assertThat(config.dataFileExtension()).isEqualTo(FileExtensions.AON);
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class DataConfigs {

        @Test
        void putAndGetByUuid() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var data = new SampleData(UUID.randomUUID(), 5);

            assertThat(config.put(data)).isTrue();

            assertThat(await(config.get(data.uuid()))).isSameAs(data);
            assertThat(config.now(data.uuid())).isSameAs(data);
            assertThat(config.all()).containsExactly(data);
            assertThat(config.uuidDataMap()).containsKey(data.uuid());
            assertThat(config.filenameDataMap()).containsKey(data.uuid() + ".json");
            assertThat(config.isData()).isTrue();
        }

        @Test
        void putRejectsOtherClasses() {
            Config<SampleData> config = register(SampleData.class, new Volatile());

            assertThat(config.put(new SampleNamedData(UUID.randomUUID(), "other"))).isFalse();

            assertThat(config.all()).isEmpty();
        }

        @Test
        void putReplacesButPutUnsafeWithoutForceDoesNot() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var uuid = UUID.randomUUID();
            var first = new SampleData(uuid, 1);
            var second = new SampleData(uuid, 2);
            config.put(first);

            config.putUnsafe(second, false);
            assertThat(config.now(uuid)).isSameAs(first);

            config.put(second);
            assertThat(config.now(uuid)).isSameAs(second);
        }

        @Test
        void putListenerCanVetoAPut() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            config.addPutListener(data -> data.value < 0);
            var accepted = new SampleData(UUID.randomUUID(), 1);
            var vetoed = new SampleData(UUID.randomUUID(), -1);

            config.put(accepted);
            config.put(vetoed);

            assertThat(config.all()).containsExactly(accepted);
            assertThat(config.onPut(vetoed)).isTrue();
        }

        @Test
        void getForMissingDataIsEmpty() {
            Config<SampleData> config = register(SampleData.class, new Volatile());

            assertThat(await(config.get(UUID.randomUUID()))).isNull();
            assertThat(await(config.get((UUID) null))).isNull();
            assertThat(config.now((UUID) null)).isNull();
        }

        @Test
        void evictForgetsData() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var data = new SampleData(UUID.randomUUID(), 1);
            config.put(data);

            config.evict(data);

            assertThat(config.all()).isEmpty();
            assertThat(config.uuidDataMap()).isEmpty();
            assertThat(config.filenameDataMap()).isEmpty();
        }

        @Test
        void clearAllForgetsEverything() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            config.put(new SampleNamedData(UUID.randomUUID(), "a"));

            config.clearAll();

            assertThat(config.all()).isEmpty();
            assertThat(config.nameDataMap()).isEmpty();
        }

        @Test
        void removeDeletesAndFiresDeleteEvent() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var data = new SampleData(UUID.randomUUID(), 1);
            config.put(data);
            var deleted = recordObjects(UEvent.DELETE_CONFIG);

            assertThat(await(config.remove(data.uuid()))).isSameAs(data);

            assertThat(config.all()).isEmpty();
            assertThat(deleted).containsExactly(data);
        }

        @Test
        void removingMissingDataCompletesEmpty() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var deleted = recordObjects(UEvent.DELETE_CONFIG);

            assertThat(await(config.remove(UUID.randomUUID()))).isNull();

            assertThat(deleted).containsExactly((Configuration) null);
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class NamedDataConfigs {

        @Test
        void getByName() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            var data = new SampleNamedData(UUID.randomUUID(), "alpha");

            config.put(data);

            assertThat(config.get("alpha")).isSameAs(data);
            assertThat(config.get("beta")).isNull();
        }

        @Test
        void updateNameMovesTheNameMapping() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            var data = new SampleNamedData(UUID.randomUUID(), "alpha");
            config.put(data);
            var oldFilename = config.filename(data);

            data.setName("beta");
            config.updateName(data, "alpha", oldFilename);

            assertThat(config.get("alpha")).isNull();
            assertThat(config.get("beta")).isSameAs(data);
        }

        @Test
        void removeByName() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            var data = new SampleNamedData(UUID.randomUUID(), "alpha");
            config.put(data);

            assertThat(await(config.remove("alpha"))).isSameAs(data);

            assertThat(config.get("alpha")).isNull();
            assertThat(config.all()).isEmpty();
        }

        @Test
        void removingAMissingNameCompletesEmpty() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());

            assertThat(await(config.remove("missing"))).isNull();
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class PlayerDataConfigs {

        @Test
        void isPlayerData() {
            var config = register(SamplePlayerData.class, new Volatile());

            assertThat(config.isPlayerData()).isTrue();
            assertThat(config.classes().<SamplePlayerData>playerDataClass()).contains(SamplePlayerData.class);
        }

        @Test
        void missingPlayerDataIsReadFromStorage() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Volatile());
            var read = recordObjects(UEvent.READ_CONFIG);
            var uuid = UUID.randomUUID();

            assertThat(await(config.get(uuid))).isNull();

            assertThat(read).containsExactly((Configuration) null);
        }

        @Test
        void deferredWritesAreMarkedForSaving() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Volatile());
            var data = new SamplePlayerData(UUID.randomUUID());
            config.put(data);

            assertThat(await(config.write(data))).isTrue();

            assertThat(config.markSaved(data.uuid())).isTrue();
            assertThat(config.markSaved(data.uuid())).isFalse();
        }

        @Test
        void asyncSaveWritesDeferredData() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Flatfile());
            var data = new SamplePlayerData(UUID.randomUUID());
            config.put(data);
            await(config.write(data));
            var file = ConfigTestSupport.modDir().resolve("players").resolve(data.uuid() + ".json");
            assertThat(file).as("the write is deferred").doesNotExist();

            config.asyncSave();

            ConfigTestSupport.awaitContent(file, data.uuid().toString());
            assertThat(config.markSaved(data.uuid())).as("asyncSave consumes the pending saves").isFalse();
        }

        @Test
        void immediateWritesAreNotDeferred() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Volatile());
            var data = new SamplePlayerData(UUID.randomUUID());
            var written = recordObjects(UEvent.WRITE_CONFIG);

            assertThat(await(config.write(data, true))).isTrue();

            assertThat(config.markSaved(data.uuid())).isFalse();
            assertThat(written).containsExactly(data);
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Events {

        @Test
        void readFiresReadEventWithTheConfiguration() {
            var config = register(SampleConfiguration.class, new Volatile());
            var read = recordObjects(UEvent.READ_CONFIG);

            assertThat(await(config.read())).isTrue();

            assertThat(read).containsExactly(config.get());
        }

        @Test
        void afterLoadTasksRunOnRead() {
            var config = register(SampleConfiguration.class, new Volatile());
            var runs = new ArrayList<String>();
            config.addAfterLoadTask(() -> runs.add("loaded"));

            await(config.read());

            assertThat(runs).containsExactly("loaded");
        }

        @Test
        void writeFiresWriteEventWithTheConfiguration() {
            var config = register(SampleConfiguration.class, new Volatile());
            var written = recordObjects(UEvent.WRITE_CONFIG);

            assertThat(await(config.write())).isTrue();

            assertThat(written).containsExactly(config.get());
        }

        @Test
        void writingAnInstanceOfAnotherClassDoesNothing() {
            var config = register(SampleConfiguration.class, new Volatile());
            var written = recordObjects(UEvent.WRITE_CONFIG);

            assertThat(await(((Config<Configuration>) (Config<?>) config).write(new SampleLang()))).isFalse();

            assertThat(written).isEmpty();
        }

        @Test
        void registerFiresRegisterEventWithThePath() {
            var paths = new ArrayList<String>();
            Mods.modLoader().event(UEvent.REGISTER_CONFIG).listen(payload -> {
                paths.add(payload.modId() + "/" + payload.path());
                return InteractionResult.PASS;
            });

            register(SampleConfiguration.class, new Volatile());

            assertThat(paths).containsExactly(ConfigFixtures.MOD + "/sampleconfiguration.json");
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Lang {

        @Test
        void formatsMappedStrings() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.format("greeting", "World").get()).isEqualTo("Hello World");
            assertThat(config.format("GREETING", "World").get()).as("keys are case-insensitive").isEqualTo("Hello World");
        }

        @Test
        void mapsMessagesWithArticleAndPlural() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.message("item").get(true, true).get()).isEqualTo("a swords");
            assertThat(config.message("item").get(false, false).get()).isEqualTo("sword");
        }

        @Test
        void unknownKeysAreEmpty() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.format("missing").get()).isEmpty();
        }

        @Test
        void remapPicksUpChangedValues() {
            var config = register(SampleLang.class, new Volatile(), true, false);
            config.remapLang();

            ((SampleLang) config.get()).greeting = "Hi %1";
            config.remapLang();

            assertThat(config.format("greeting", "there").get()).isEqualTo("Hi there");
        }

        @Test
        void nonLangConfigsMapNothing() {
            var config = register(SampleLang.class, new Volatile(), false, false);

            config.remapLang();

            assertThat(config.isLang()).isFalse();
            assertThat(config.format("greeting", "World").get()).isEmpty();
        }
    }

    private static List<Configuration> recordObjects(de.waterdu.atlantis.util.config.Key<? extends UEvent<? extends de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload>> key) {
        var objects = new ArrayList<Configuration>();
        if (key.equals(UEvent.READ_CONFIG)) {
            Mods.modLoader().event(UEvent.READ_CONFIG).listen(payload -> add(objects, payload.object()));
        } else if (key.equals(UEvent.WRITE_CONFIG)) {
            Mods.modLoader().event(UEvent.WRITE_CONFIG).listen(payload -> add(objects, payload.object()));
        } else if (key.equals(UEvent.DELETE_CONFIG)) {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).listen(payload -> add(objects, payload.object()));
        }
        return objects;
    }

    private static InteractionResult add(List<Configuration> objects, Configuration object) {
        synchronized (objects) {
            objects.add(object);
        }
        return InteractionResult.PASS;
    }
}
