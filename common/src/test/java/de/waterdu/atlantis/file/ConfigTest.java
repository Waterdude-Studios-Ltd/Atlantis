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

/**
 * Tests {@link Config}: data maps, named data, player data, config events and lang files.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class ConfigTest {

    /**
     * Checks that a registered config reports its owner, key, name, instance, data flags and file extension.
     */
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

    /**
     * Checks that an AON config reports {@link FileExtensions#AON} as its data file extension.
     */
    @Test
    void aonConfigsUseTheAonExtension() {
        var config = register(SampleConfiguration.class, "sample.aon", new Volatile(), false, true);

        assertThat(config.isAON()).isTrue();
        assertThat(config.dataFileExtension()).isEqualTo(FileExtensions.AON);
    }

    /**
     * Tests configs that hold {@link SampleData}.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class DataConfigs {

        /**
         * Checks that put data is found by UUID and appears in the UUID and filename maps.
         */
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

        /**
         * Checks that AON data uses the {@code .aon} extension in its file name and in the filename map.
         */
        @Test
        void aonDataUsesTheAonFileName() {
            Config<SampleData> config = register(SampleData.class, new Volatile(), false, true);
            var data = new SampleData(UUID.randomUUID(), 5);

            config.put(data);

            assertThat(config.filename(data)).isEqualTo(data.uuid() + FileExtensions.AON);
            assertThat(config.filenameDataMap()).containsOnlyKeys(data.uuid() + FileExtensions.AON);
        }

        /**
         * Checks that a put of an instance of another class, even a subclass, returns {@code false} and adds
         * nothing.
         */
        @Test
        void putRejectsOtherClasses() {
            Config<SampleData> config = register(SampleData.class, new Volatile());

            assertThat(config.put(new SampleNamedData(UUID.randomUUID(), "other"))).isFalse();

            assertThat(config.all()).isEmpty();
        }

        /**
         * Checks that {@code put} replaces data with the same UUID, but {@code putUnsafe} without force does not.
         */
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

        /**
         * Checks that a put listener that returns {@code true} stops the put.
         */
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

        /**
         * Checks that a get for an unknown or {@code null} UUID gives {@code null}.
         */
        @Test
        void getForMissingDataIsEmpty() {
            Config<SampleData> config = register(SampleData.class, new Volatile());

            assertThat(await(config.get(UUID.randomUUID()))).isNull();
            assertThat(await(config.get((UUID) null))).isNull();
            assertThat(config.now((UUID) null)).isNull();
        }

        /**
         * Checks that {@code evict} removes the data from every map.
         */
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

        /**
         * Checks that {@code clearAll} removes all data, including the name map.
         */
        @Test
        void clearAllForgetsEverything() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            config.put(new SampleNamedData(UUID.randomUUID(), "a"));

            config.clearAll();

            assertThat(config.all()).isEmpty();
            assertThat(config.nameDataMap()).isEmpty();
        }

        /**
         * Checks that {@code remove} by UUID gives the removed data and fires {@link UEvent#DELETE_CONFIG} with it.
         */
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

        /**
         * Checks that the removal of unknown data gives {@code null} and fires {@link UEvent#DELETE_CONFIG} with
         * {@code null}.
         */
        @Test
        void removingMissingDataCompletesEmpty() {
            Config<SampleData> config = register(SampleData.class, new Volatile());
            var deleted = recordObjects(UEvent.DELETE_CONFIG);

            assertThat(await(config.remove(UUID.randomUUID()))).isNull();

            assertThat(deleted).containsExactly((Configuration) null);
        }
    }

    /**
     * Tests configs that hold {@link SampleNamedData}.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class NamedDataConfigs {

        /**
         * Checks that put data is found by its name, and an unknown name gives {@code null}.
         */
        @Test
        void getByName() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            var data = new SampleNamedData(UUID.randomUUID(), "alpha");

            config.put(data);

            assertThat(config.get("alpha")).isSameAs(data);
            assertThat(config.get("beta")).isNull();
        }

        /**
         * Checks that {@code updateName} maps the data to the new name and removes the old name.
         */
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

        /**
         * Checks that {@code remove} by name gives the removed data and removes it from the config.
         */
        @Test
        void removeByName() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());
            var data = new SampleNamedData(UUID.randomUUID(), "alpha");
            config.put(data);

            assertThat(await(config.remove("alpha"))).isSameAs(data);

            assertThat(config.get("alpha")).isNull();
            assertThat(config.all()).isEmpty();
        }

        /**
         * Checks that the removal of an unknown name gives {@code null}.
         */
        @Test
        void removingAMissingNameCompletesEmpty() {
            Config<SampleNamedData> config = register(SampleNamedData.class, new Volatile());

            assertThat(await(config.remove("missing"))).isNull();
        }
    }

    /**
     * Tests configs that hold {@link SamplePlayerData}.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class PlayerDataConfigs {

        /**
         * Checks that the config reports itself as player data and gives the player data class.
         */
        @Test
        void isPlayerData() {
            var config = register(SamplePlayerData.class, new Volatile());

            assertThat(config.isPlayerData()).isTrue();
            assertThat(config.classes().<SamplePlayerData>playerDataClass()).isEqualTo(SamplePlayerData.class);
        }

        /**
         * Checks that a get for unknown player data reads from storage, which fires {@link UEvent#READ_CONFIG}.
         */
        @Test
        void missingPlayerDataIsReadFromStorage() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Volatile());
            var read = recordObjects(UEvent.READ_CONFIG);
            var uuid = UUID.randomUUID();

            assertThat(await(config.get(uuid))).isNull();

            assertThat(read).containsExactly((Configuration) null);
        }

        /**
         * Checks that a normal write of player data marks it for saving once.
         */
        @Test
        void deferredWritesAreMarkedForSaving() {
            Config<SamplePlayerData> config = register(SamplePlayerData.class, new Volatile());
            var data = new SamplePlayerData(UUID.randomUUID());
            config.put(data);

            assertThat(await(config.write(data))).isTrue();

            assertThat(config.markSaved(data.uuid())).isTrue();
            assertThat(config.markSaved(data.uuid())).isFalse();
        }

        /**
         * Checks that a deferred write creates no file until {@code asyncSave} runs, and that
         * {@code asyncSave} clears the pending saves.
         */
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

        /**
         * Checks that an immediate write fires {@link UEvent#WRITE_CONFIG} and does not mark the data for saving.
         */
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

    /**
     * Tests the events and tasks that configs fire when they register, read and write.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Events {

        /**
         * Checks that {@code read} fires {@link UEvent#READ_CONFIG} with the configuration.
         */
        @Test
        void readFiresReadEventWithTheConfiguration() {
            var config = register(SampleConfiguration.class, new Volatile());
            var read = recordObjects(UEvent.READ_CONFIG);

            assertThat(await(config.read())).isTrue();

            assertThat(read).containsExactly(config.get());
        }

        /**
         * Checks that an after-load task runs once when the config is read.
         */
        @Test
        void afterLoadTasksRunOnRead() {
            var config = register(SampleConfiguration.class, new Volatile());
            var runs = new ArrayList<String>();
            config.addAfterLoadTask(() -> runs.add("loaded"));

            await(config.read());

            assertThat(runs).containsExactly("loaded");
        }

        /**
         * Checks that {@code write} fires {@link UEvent#WRITE_CONFIG} with the configuration.
         */
        @Test
        void writeFiresWriteEventWithTheConfiguration() {
            var config = register(SampleConfiguration.class, new Volatile());
            var written = recordObjects(UEvent.WRITE_CONFIG);

            assertThat(await(config.write())).isTrue();

            assertThat(written).containsExactly(config.get());
        }

        /**
         * Checks that a write of an instance of another class gives {@code false} and fires no event.
         */
        @Test
        void writingAnInstanceOfAnotherClassDoesNothing() {
            var config = register(SampleConfiguration.class, new Volatile());
            var written = recordObjects(UEvent.WRITE_CONFIG);

            assertThat(await(((Config<Configuration>) (Config<?>) config).write(new SampleLang()))).isFalse();

            assertThat(written).isEmpty();
        }

        /**
         * Checks that registration fires {@link UEvent#REGISTER_CONFIG} with the mod ID and the path.
         */
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

    /**
     * Tests lang configs that hold {@link SampleLang}.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Lang {

        /**
         * Checks that {@code format} fills in the arguments of a mapped string, and that keys ignore case.
         */
        @Test
        void formatsMappedStrings() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.format("greeting", "World").get()).isEqualTo("Hello World");
            assertThat(config.format("GREETING", "World").get()).as("keys are case-insensitive").isEqualTo("Hello World");
        }

        /**
         * Checks that a mapped {@link Message} adds its article and plural only when asked.
         */
        @Test
        void mapsMessagesWithArticleAndPlural() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.message("item").get(true, true).get()).isEqualTo("a swords");
            assertThat(config.message("item").get(false, false).get()).isEqualTo("sword");
        }

        /**
         * Checks that an unknown key formats to an empty string.
         */
        @Test
        void unknownKeysAreEmpty() {
            var config = register(SampleLang.class, new Volatile(), true, false);

            config.remapLang();

            assertThat(config.format("missing").get()).isEmpty();
        }

        /**
         * Checks that {@code remapLang} uses values that changed after the last remap.
         */
        @Test
        void remapPicksUpChangedValues() {
            var config = register(SampleLang.class, new Volatile(), true, false);
            config.remapLang();

            ((SampleLang) config.get()).greeting = "Hi %1";
            config.remapLang();

            assertThat(config.format("greeting", "there").get()).isEqualTo("Hi there");
        }

        /**
         * Checks that {@code remapLang} maps nothing for a config that is not a lang file.
         */
        @Test
        void nonLangConfigsMapNothing() {
            var config = register(SampleLang.class, new Volatile(), false, false);

            config.remapLang();

            assertThat(config.isLang()).isFalse();
            assertThat(config.format("greeting", "World").get()).isEmpty();
        }
    }

    /**
     * Listens to a config event and records the object of each payload.
     *
     * @param key the event, which is {@link UEvent#READ_CONFIG}, {@link UEvent#WRITE_CONFIG} or
     *     {@link UEvent#DELETE_CONFIG}. Other events record nothing.
     * @return the list that the listener adds the objects to
     */
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

    /**
     * Adds the object to the list while it holds the lock of the list.
     *
     * @param objects the list to add to
     * @param object the object to add
     * @return {@link InteractionResult#PASS}, so the event continues
     */
    private static InteractionResult add(List<Configuration> objects, Configuration object) {
        synchronized (objects) {
            objects.add(object);
        }
        return InteractionResult.PASS;
    }
}
