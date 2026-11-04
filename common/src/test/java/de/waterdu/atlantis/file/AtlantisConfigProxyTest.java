package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.ConfigFixtures.SampleConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.SampleData;
import de.waterdu.atlantis.file.ConfigFixtures.SampleLang;
import de.waterdu.atlantis.file.ConfigFixtures.SampleNamedData;
import de.waterdu.atlantis.file.ConfigFixtures.SamplePlayerData;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Volatile;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.waterdu.atlantis.file.ConfigTestSupport.await;
import static de.waterdu.atlantis.file.ConfigTestSupport.awaitUntil;
import static de.waterdu.atlantis.file.ConfigTestSupport.modDir;
import static de.waterdu.atlantis.file.ConfigTestSupport.register;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link AtlantisConfigProxy}.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisConfigProxyTest {

    /**
     * @return the config proxy of the test mod
     */
    private static AtlantisConfigProxy.Impl proxy() {
        return AtlantisConfigProxy.of(ConfigFixtures.MOD);
    }

    /**
     * Makes the configs of the test mod ready with {@code AtlantisConfig.postRegistration}.
     */
    private static void markReady() {
        AtlantisConfig.postRegistration(ConfigFixtures.MOD);
    }

    /**
     * Checks that the same mod ID always gives the same proxy, and that the proxy wraps the mod's
     * {@link AtlantisConfig}.
     */
    @Test
    void sameModGivesTheSameProxy() {
        assertThat(AtlantisConfigProxy.of(ConfigFixtures.MOD)).isSameAs(proxy());
        assertThat(proxy().internal()).isSameAs(AtlantisConfig.of(ConfigFixtures.MOD));
    }

    /**
     * Checks that the proxy finds a registered config by its configuration class.
     */
    @Test
    void configLooksUpByClass() {
        var config = register(SampleConfiguration.class, new Volatile());

        assertThat(proxy().config(SampleConfiguration.class)).isSameAs(config);
    }

    /**
     * Checks that a get before the configs are ready reads the file at once.
     *
     * @throws Exception if the file cannot be written
     */
    @Test
    void getBeforeReadyReadsImmediately() throws Exception {
        register(SampleConfiguration.class, new Flatfile());
        Files.writeString(modDir().resolve("sampleconfiguration.json"), "{\"number\": 42}");

        SampleConfiguration configuration = proxy().get(SampleConfiguration.class);

        assertThat(configuration.number).isEqualTo(42);
    }

    /**
     * Checks that a get before the configs are ready gives the defaults and writes them when there is no file.
     */
    @Test
    void getBeforeReadyWritesDefaultsWhenNothingWasRead() {
        register(SampleConfiguration.class, new Flatfile());

        SampleConfiguration configuration = proxy().get(SampleConfiguration.class);

        assertThat(configuration.number).isEqualTo(1);
        awaitUntil("defaults are written", () -> ConfigTestSupport.read(modDir().resolve("sampleconfiguration.json")).contains("\"number\": 1"));
    }

    /**
     * Checks that both {@code orElse} methods give the fallback before the configs are ready.
     */
    @Test
    void orElseBeforeReadyGivesTheFallback() {
        register(SampleConfiguration.class, new Volatile());
        var fallback = new SampleConfiguration();

        assertThat(proxy().orElse(SampleConfiguration.class, fallback)).isSameAs(fallback);
        assertThat(proxy().orElse(fallback)).isSameAs(fallback);
    }

    /**
     * Checks that {@code orElse} and {@code get} give the loaded configuration after the configs are ready.
     */
    @Test
    void orElseWhenReadyGivesTheConfig() {
        var config = register(SampleConfiguration.class, new Volatile());
        markReady();

        assertThat(proxy().orElse(SampleConfiguration.class, new SampleConfiguration())).isSameAs(config.get());
        assertThat(proxy().get(SampleConfiguration.class)).isSameAs(config.get());
    }

    /**
     * Checks that put data is found by UUID, by name and by instance, and that it is in the collection of all data.
     */
    @Test
    void putContainsAndAll() {
        register(SampleNamedData.class, new Volatile());
        var data = new SampleNamedData(UUID.randomUUID(), "alpha");

        assertThat(proxy().isEmpty(SampleNamedData.class)).isTrue();
        proxy().put(data);

        assertThat(proxy().contains(SampleNamedData.class, data.uuid())).isTrue();
        assertThat(proxy().contains(SampleNamedData.class, "alpha")).isTrue();
        assertThat(proxy().contains(SampleNamedData.class, data)).isTrue();
        assertThat(proxy().all(SampleNamedData.class)).containsExactly(data);
        assertThat(proxy().isEmpty(SampleNamedData.class)).isFalse();
    }

    /**
     * Checks that a put listener that returns {@code true} stops the put.
     */
    @Test
    void addPutListenerVetoesPuts() {
        register(SampleData.class, new Volatile());
        proxy().<SampleData>addPutListener(SampleData.class, data -> data.value < 0);

        proxy().put(new SampleData(UUID.randomUUID(), -1));

        assertThat(proxy().isEmpty(SampleData.class)).isTrue();
    }

    /**
     * Checks that named data is found by name, and that {@code now} gives {@code null} for an unknown name.
     */
    @Test
    void namedDataByName() {
        register(SampleNamedData.class, new Volatile());
        var data = new SampleNamedData(UUID.randomUUID(), "alpha");
        proxy().put(data);

        assertThat(await(proxy().get(SampleNamedData.class, "alpha"))).isSameAs(data);
        assertThat(proxy().now(SampleNamedData.class, "alpha")).isSameAs(data);
        assertThat(proxy().now(SampleNamedData.class, "beta")).isNull();
    }

    /**
     * Checks that data is found by UUID with {@code get} and with {@code now}.
     */
    @Test
    void dataByUuid() {
        register(SampleData.class, new Volatile());
        var data = new SampleData(UUID.randomUUID(), 1);
        proxy().put(data);

        assertThat(await(proxy().get(SampleData.class, data.uuid()))).isSameAs(data);
        assertThat(proxy().now(SampleData.class, data.uuid())).isSameAs(data);
    }

    /**
     * Checks that {@code getForOrPut} creates and puts missing data, but keeps data that already exists.
     */
    @Test
    void getForOrPutCreatesMissingData() {
        register(SamplePlayerData.class, new Volatile());
        var uuid = UUID.randomUUID();
        var created = new SamplePlayerData(uuid);

        assertThat(await(proxy().getForOrPut(SamplePlayerData.class, uuid, () -> created))).isSameAs(created);
        assertThat(await(proxy().getForOrPut(SamplePlayerData.class, uuid, SamplePlayerData::new))).as("existing data is kept").isSameAs(created);
        assertThat(proxy().now(SamplePlayerData.class, uuid)).isSameAs(created);
    }

    /**
     * Checks that data can be removed by UUID and by name, and that each remove gives the removed data.
     */
    @Test
    void removeByUuidAndName() {
        register(SampleNamedData.class, new Volatile());
        var first = new SampleNamedData(UUID.randomUUID(), "first");
        var second = new SampleNamedData(UUID.randomUUID(), "second");
        proxy().put(first);
        proxy().put(second);

        assertThat(await(proxy().remove(SampleNamedData.class, first.uuid()))).isSameAs(first);
        assertThat(await(proxy().remove(SampleNamedData.class, "second"))).isSameAs(second);

        assertThat(proxy().isEmpty(SampleNamedData.class)).isTrue();
    }

    /**
     * Checks that {@code format}, {@code formatPositive} and {@code message} read lang configs, and give empty text
     * for other configs.
     */
    @Test
    void formatsOnlyLangConfigs() {
        register(SampleLang.class, new Volatile(), true, false).remapLang();
        register(SampleConfiguration.class, new Volatile());

        assertThat(proxy().format(SampleLang.class, "greeting", "World").get()).isEqualTo("Hello World");
        assertThat(proxy().formatPositive(SampleLang.class, "greeting", "World").get()).isEqualTo("[+] Hello World");
        assertThat(proxy().message(SampleLang.class, "item").get(true, false).get()).isEqualTo("a sword");
        assertThat(proxy().format(SampleConfiguration.class, "text").get()).isEmpty();
        assertThat(proxy().message(SampleConfiguration.class, "text").get(false, false).get()).isEmpty();
    }

    /**
     * Checks that both {@code enqueue} methods run the task at once when the configs are ready.
     */
    @Test
    void enqueueRunsImmediatelyWhenReady() {
        register(SampleData.class, new Volatile());
        markReady();
        var runs = new ArrayList<String>();

        proxy().enqueue(() -> runs.add("task"));
        proxy().enqueue(SampleData.class, all -> runs.add("data:" + all.size()));

        assertThat(runs).containsExactly("task", "data:0");
    }

    /**
     * Checks that a task queued before the configs are ready runs only after they are ready.
     */
    @Test
    void enqueueBeforeReadyWaitsUntilReady() {
        register(SampleData.class, new Volatile());
        var runs = Collections.synchronizedList(new ArrayList<String>());

        proxy().enqueue(() -> runs.add("task"));
        assertThat(runs).isEmpty();

        markReady();
        awaitUntil("the queued task runs", () -> runs.contains("task"));
    }

    /**
     * Checks that {@code loadAll} on the proxy reads only the configs of the data type.
     *
     * @param type the data type to load
     * @param expected the simple names of the configs that must be read, separated by {@code ;}
     */
    @ParameterizedTest
    @CsvSource({
            "ALL,           SampleConfiguration;SampleData;SamplePlayerData",
            "CONFIGURATION, SampleConfiguration",
            "ALL_DATA,      SampleData;SamplePlayerData",
            "DATA,          SampleData",
            "PLAYER_DATA,   SamplePlayerData",
    })
    void loadAllReadsTheMatchingConfigs(DataType type, String expected) {
        registerOneOfEachType();
        var read = recordConfigs(UEvent.READ_CONFIG);

        proxy().loadAll(type);

        awaitUntil("every matching config is read", () -> read.size() >= expected.split(";").length);
        assertThat(read).containsExactlyInAnyOrder(expected.split(";"));
    }

    /**
     * Checks that {@code saveAll} on the proxy writes only the configs of the data type.
     *
     * @param type the data type to save
     * @param expected the simple names of the configs that must be written, separated by {@code ;}
     */
    @ParameterizedTest
    @CsvSource({
            "ALL,           SampleConfiguration;SampleData;SamplePlayerData",
            "CONFIGURATION, SampleConfiguration",
            "PLAYER_DATA,   SamplePlayerData",
    })
    void saveAllWritesTheMatchingConfigs(DataType type, String expected) {
        registerOneOfEachType();
        var written = recordConfigs(UEvent.WRITE_CONFIG);

        proxy().saveAll(type);

        awaitUntil("every matching config is written", () -> written.size() >= expected.split(";").length);
        assertThat(written).containsExactlyInAnyOrder(expected.split(";"));
    }

    /**
     * Checks that the static {@code AtlantisConfig.loadAll} reads the same configs as {@code loadAll} on the proxy.
     *
     * @param type the data type to load
     * @param expected the simple names of the configs that must be read, separated by {@code ;}
     */
    @ParameterizedTest
    @CsvSource({
            "ALL,           SampleConfiguration;SampleData;SamplePlayerData",
            "CONFIGURATION, SampleConfiguration",
            "ALL_DATA,      SampleData;SamplePlayerData",
            "DATA,          SampleData",
            "PLAYER_DATA,   SamplePlayerData",
    })
    void staticLoadAllAgreesWithTheProxy(DataType type, String expected) {
        registerOneOfEachType();
        var read = recordConfigs(UEvent.READ_CONFIG);

        AtlantisConfig.loadAll(ConfigFixtures.MOD, type);

        awaitUntil("every matching config is read", () -> read.size() >= expected.split(";").length);
        assertThat(read).containsExactlyInAnyOrder(expected.split(";"));
    }

    /**
     * Registers a configuration, a data config and a player data config, each with volatile storage.
     */
    private static void registerOneOfEachType() {
        register(SampleConfiguration.class, new Volatile());
        register(SampleData.class, new Volatile());
        register(SamplePlayerData.class, new Volatile());
    }

    /**
     * Listens for the event and records the simple name of the configuration class of each config it fires for.
     *
     * @param key {@link UEvent#READ_CONFIG}, or any other key to listen for {@link UEvent#WRITE_CONFIG}
     * @return a synchronized list that fills as the event fires
     */
    private static List<String> recordConfigs(de.waterdu.atlantis.util.config.Key<? extends UEvent<? extends de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload>> key) {
        var names = Collections.synchronizedList(new ArrayList<String>());
        if (key.equals(UEvent.READ_CONFIG)) {
            Mods.modLoader().event(UEvent.READ_CONFIG).listen(payload -> add(names, payload.config()));
        } else {
            Mods.modLoader().event(UEvent.WRITE_CONFIG).listen(payload -> add(names, payload.config()));
        }
        return names;
    }

    /**
     * Adds the simple name of the config's configuration class to the list.
     *
     * @param names the list to add to
     * @param config the config to record
     * @return {@link InteractionResult#PASS}, so that the other listeners still run
     */
    private static InteractionResult add(List<String> names, Config<?> config) {
        names.add(config.classes().simpleName());
        return InteractionResult.PASS;
    }
}
