package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.StorageTestConfigs;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static de.waterdu.atlantis.file.StorageTestConfigs.await;
import static de.waterdu.atlantis.file.StorageTestConfigs.awaitUntil;
import static de.waterdu.atlantis.file.StorageTestConfigs.modDir;
import static de.waterdu.atlantis.file.StorageTestConfigs.read;
import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class FlatfileTest {

    @Test
    void replaceOverwritesTheTarget(@TempDir Path dir) throws IOException {
        var source = dir.resolve("a.json.temp");
        var target = dir.resolve("a.json");
        Files.writeString(source, "new");
        Files.writeString(target, "old");

        assertThat(Flatfile.replace(source.toFile(), target.toFile())).isTrue();

        assertThat(target).hasContent("new");
        assertThat(source).doesNotExist();
    }

    @Test
    void replaceNeverLeavesTheTargetMissing(@TempDir Path dir) throws Exception {
        var source = dir.resolve("a.json.temp");
        var target = dir.resolve("a.json");
        Files.writeString(target, "0");
        var done = new AtomicBoolean();
        var missing = new AtomicInteger();
        var reader = new Thread(() -> {
            while (!done.get()) {
                if (!Files.exists(target)) {
                    missing.incrementAndGet();
                }
            }
        });
        reader.start();
        try {
            for (var i = 0; i < 2000; i++) {
                Files.writeString(source, Integer.toString(i));
                assertThat(Flatfile.replace(source.toFile(), target.toFile())).isTrue();
            }
        } finally {
            done.set(true);
            reader.join();
        }

        assertThat(missing).hasValue(0);
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class SingleFile {

        @Test
        void writesTheConfigurationAsJson() {
            var config = StorageTestConfigs.single(new Flatfile());

            assertThat(await(config.write())).isTrue();

            assertThat(read(file())).contains("\"number\": 1", "\"text\": \"default\"");
        }

        @Test
        void readsChangesFromDisk() throws IOException {
            var config = StorageTestConfigs.single(new Flatfile());
            Files.writeString(file(), "{\"number\": 7, \"text\": \"edited\"}");

            assertThat(await(config.read())).isTrue();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(7);
        }

        @Test
        void readingAMissingFileKeepsTheDefaults() {
            var config = StorageTestConfigs.single(new Flatfile());

            assertThat(await(config.read(true))).as("nothing was read").isFalse();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(1);
            assertThat(file()).as("the file is created for the next write").exists();
        }

        @Test
        void corruptFileIsMarkedInvalidAndDefaultsAreKept() throws IOException {
            var config = StorageTestConfigs.single(new Flatfile());
            Files.writeString(file(), "{\"number\": \"not a number\"}");

            assertThat(await(config.read())).isFalse();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(1);
            assertThat(file()).doesNotExist();
            assertThat(read(file().resolveSibling("sampleconfiguration.json.invalid"))).contains("not a number");
        }

        @Test
        void aRepeatedlyCorruptFileGetsNumberedInvalidCopies() throws IOException {
            var config = StorageTestConfigs.single(new Flatfile());
            for (var i = 0; i < 2; i++) {
                Files.writeString(file(), "{\"number\": \"broken " + i + "\"}");
                await(config.read());
            }

            assertThat(read(file().resolveSibling("sampleconfiguration.json.invalid"))).contains("broken 0");
            assertThat(read(file().resolveSibling("sampleconfiguration.json (1).invalid"))).contains("broken 1");
        }

        private static Path file() {
            return modDir().resolve("sampleconfiguration.json");
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Aon {

        @Test
        void writesComments() {
            var config = StorageTestConfigs.aon(new Flatfile());

            await(config.write());

            assertThat(read(file())).contains("# Sample AON file", "# Second line", "# How many things", "count: 3");
        }

        @Test
        void readsAnEditedFileAndKeepsItsComments() throws IOException {
            var config = StorageTestConfigs.aon(new Flatfile());
            await(config.write());
            Files.writeString(file(), read(file()).replace("count: 3", "count: 9"));

            assertThat(await(config.read())).isTrue();
            assertThat(StorageTestConfigs.count(config.get())).isEqualTo(9);

            await(config.write());
            assertThat(read(file())).contains("# Sample AON file", "# How many things", "count: 9");
        }

        @Test
        void migratesAnExistingJsonFile() throws IOException {
            var config = StorageTestConfigs.aon(new Flatfile());
            var json = modDir().resolve("commented.json");
            Files.writeString(json, "{\"count\": 5}");

            assertThat(await(config.read())).isTrue();
            assertThat(StorageTestConfigs.count(config.get())).isEqualTo(5);

            await(config.write());
            assertThat(read(file())).contains("count: 5");
            assertThat(json).doesNotExist();
        }

        private static Path file() {
            return modDir().resolve("commented.aon");
        }
    }

    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class DataFiles {

        @Test
        void writeSpecificWritesOneFilePerInstance() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(4);

            assertThat(await(config.storage().writeSpecific(config, data))).isTrue();

            assertThat(read(file(data.uuid().toString()))).contains("\"value\": 4");
            assertThat(dir().resolve(data.uuid() + ".json.temp")).doesNotExist();
        }

        @Test
        void writeWritesEveryInstance() {
            var config = StorageTestConfigs.data(new Flatfile());
            var first = StorageTestConfigs.newData(1);
            var second = StorageTestConfigs.newData(2);
            config.put(first);
            config.put(second);

            assertThat(await(config.write())).isTrue();

            awaitUntil("both data files are written", () ->
                    read(file(first.uuid().toString())).contains("\"value\": 1") && read(file(second.uuid().toString())).contains("\"value\": 2"));
        }

        @Test
        void writeWithNothingToWriteCompletes() {
            var config = StorageTestConfigs.data(new Flatfile());

            assertThat(await(config.write())).isTrue();
        }

        @Test
        void readLoadsEveryFileInTheDirectory() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var first = StorageTestConfigs.newData(1);
            var second = StorageTestConfigs.newData(2);
            await(config.storage().writeSpecific(config, first));
            await(config.storage().writeSpecific(config, second));

            assertThat(await(config.read())).isTrue();

            assertThat(config.all()).containsExactlyInAnyOrder(first, second);
            assertThat(StorageTestConfigs.value(config.now(second.uuid()))).isEqualTo(2);
        }

        @Test
        void readFromUuidLoadsOneFile() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(3);
            await(config.storage().writeSpecific(config, data));

            var read = await(config.storage().readFromUUID(config, data.uuid()));

            assertThat(read).isEqualTo(data);
            assertThat(config.uuidDataMap()).containsKey(data.uuid());
        }

        @Test
        void corruptFileReadFromUuidIsMarkedInvalid() throws IOException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(3);
            Files.writeString(file(data.uuid().toString()), "{\"value\": \"oops\"}");

            assertThat(await(config.storage().readFromUUID(config, data.uuid()))).isNull();

            assertThat(file(data.uuid().toString())).doesNotExist();
            assertThat(dir().resolve(data.uuid() + ".json.invalid")).exists();
        }

        @Test
        void corruptFileIsSkippedWhenReadingTheDirectory() throws IOException, InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var good = StorageTestConfigs.newData(1);
            await(config.storage().writeSpecific(config, good));
            var corrupt = file("corrupt");
            Files.writeString(corrupt, "{\"value\": \"oops\"}");

            assertThat(await(config.read())).isTrue();

            assertThat(config.all()).containsExactly(good);
            assertThat(corrupt).exists();
        }

        @Test
        void removeMovesTheFileToTheErrorBin() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(1);
            config.put(data);
            await(config.storage().writeSpecific(config, data));

            assertThat(await(config.remove(data.uuid()))).isSameAs(data);

            var bin = AtlantisConfig.dirOf(Atlantis.ID).toPath().resolve("bin");
            var backup = bin.resolve(data.uuid() + ".json_" + StorageTestConfigs.MOD + "_" + config.key() + ".backup");
            awaitUntil("the file is moved to the error bin", () -> Files.exists(backup));
            assertThat(file(data.uuid().toString())).doesNotExist();
            assertThat(config.all()).isEmpty();
        }

        private static Path dir() {
            return modDir().resolve("sampledata");
        }

        private static Path file(String identifier) {
            return dir().resolve(identifier + ".json");
        }
    }
}
