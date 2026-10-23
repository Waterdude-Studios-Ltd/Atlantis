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

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class AtlantisConfigProxyTest {

    private static AtlantisConfigProxy.Impl proxy() {
        return AtlantisConfigProxy.of(ConfigFixtures.MOD);
    }

    private static void markReady() {
        AtlantisConfig.postRegistration(ConfigFixtures.MOD);
    }

    @Test
    void sameModGivesTheSameProxy() {
        assertThat(AtlantisConfigProxy.of(ConfigFixtures.MOD)).isSameAs(proxy());
        assertThat(proxy().internal()).isSameAs(AtlantisConfig.of(ConfigFixtures.MOD));
    }

    @Test
    void configLooksUpByClass() {
        var config = register(SampleConfiguration.class, new Volatile());

        assertThat(proxy().config(SampleConfiguration.class)).isSameAs(config);
    }

    @Test
    void getBeforeReadyReadsImmediately() throws Exception {
        register(SampleConfiguration.class, new Flatfile());
        Files.writeString(modDir().resolve("sampleconfiguration.json"), "{\"number\": 42}");

        SampleConfiguration configuration = proxy().get(SampleConfiguration.class);

        assertThat(configuration.number).isEqualTo(42);
    }

    @Test
    void getBeforeReadyWritesDefaultsWhenNothingWasRead() {
        register(SampleConfiguration.class, new Flatfile());

        SampleConfiguration configuration = proxy().get(SampleConfiguration.class);

        assertThat(configuration.number).isEqualTo(1);
        awaitUntil("defaults are written", () -> ConfigTestSupport.read(modDir().resolve("sampleconfiguration.json")).contains("\"number\": 1"));
    }

    @Test
    void orElseBeforeReadyGivesTheFallback() {
        register(SampleConfiguration.class, new Volatile());
        var fallback = new SampleConfiguration();

        assertThat(proxy().orElse(SampleConfiguration.class, fallback)).isSameAs(fallback);
        assertThat(proxy().orElse(fallback)).isSameAs(fallback);
    }

    @Test
    void orElseWhenReadyGivesTheConfig() {
        var config = register(SampleConfiguration.class, new Volatile());
        markReady();

        assertThat(proxy().orElse(SampleConfiguration.class, new SampleConfiguration())).isSameAs(config.get());
        assertThat(proxy().get(SampleConfiguration.class)).isSameAs(config.get());
    }

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

    @Test
    void addPutListenerVetoesPuts() {
        register(SampleData.class, new Volatile());
        proxy().<SampleData>addPutListener(SampleData.class, data -> data.value < 0);

        proxy().put(new SampleData(UUID.randomUUID(), -1));

        assertThat(proxy().isEmpty(SampleData.class)).isTrue();
    }

    @Test
    void namedDataByName() {
        register(SampleNamedData.class, new Volatile());
        var data = new SampleNamedData(UUID.randomUUID(), "alpha");
        proxy().put(data);

        assertThat(await(proxy().get(SampleNamedData.class, "alpha"))).isSameAs(data);
        assertThat(proxy().now(SampleNamedData.class, "alpha")).isSameAs(data);
        assertThat(proxy().now(SampleNamedData.class, "beta")).isNull();
    }

    @Test
    void dataByUuid() {
        register(SampleData.class, new Volatile());
        var data = new SampleData(UUID.randomUUID(), 1);
        proxy().put(data);

        assertThat(await(proxy().get(SampleData.class, data.uuid()))).isSameAs(data);
        assertThat(proxy().now(SampleData.class, data.uuid())).isSameAs(data);
    }

    @Test
    void getForOrPutCreatesMissingData() {
        register(SamplePlayerData.class, new Volatile());
        var uuid = UUID.randomUUID();
        var created = new SamplePlayerData(uuid);

        assertThat(await(proxy().getForOrPut(SamplePlayerData.class, uuid, () -> created))).isSameAs(created);
        assertThat(await(proxy().getForOrPut(SamplePlayerData.class, uuid, SamplePlayerData::new))).as("existing data is kept").isSameAs(created);
        assertThat(proxy().now(SamplePlayerData.class, uuid)).isSameAs(created);
    }

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

    @Test
    void enqueueRunsImmediatelyWhenReady() {
        register(SampleData.class, new Volatile());
        markReady();
        var runs = new ArrayList<String>();

        proxy().enqueue(() -> runs.add("task"));
        proxy().enqueue(SampleData.class, all -> runs.add("data:" + all.size()));

        assertThat(runs).containsExactly("task", "data:0");
    }

    @Test
    void enqueueBeforeReadyWaitsUntilReady() {
        register(SampleData.class, new Volatile());
        var runs = Collections.synchronizedList(new ArrayList<String>());

        proxy().enqueue(() -> runs.add("task"));
        assertThat(runs).isEmpty();

        markReady();
        awaitUntil("the queued task runs", () -> runs.contains("task"));
    }

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

    private static void registerOneOfEachType() {
        register(SampleConfiguration.class, new Volatile());
        register(SampleData.class, new Volatile());
        register(SamplePlayerData.class, new Volatile());
    }

    private static List<String> recordConfigs(de.waterdu.atlantis.util.config.Key<? extends UEvent<? extends de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload>> key) {
        var names = Collections.synchronizedList(new ArrayList<String>());
        if (key.equals(UEvent.READ_CONFIG)) {
            Mods.modLoader().event(UEvent.READ_CONFIG).listen(payload -> add(names, payload.config()));
        } else {
            Mods.modLoader().event(UEvent.WRITE_CONFIG).listen(payload -> add(names, payload.config()));
        }
        return names;
    }

    private static InteractionResult add(List<String> names, Config<?> config) {
        names.add(config.classes().simpleName());
        return InteractionResult.PASS;
    }
}
