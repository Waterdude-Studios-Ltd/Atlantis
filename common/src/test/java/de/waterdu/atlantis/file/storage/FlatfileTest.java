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

/**
 * Tests {@link Flatfile} storage for single files, AON files and data directories.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class FlatfileTest {

    /**
     * Checks that {@link Flatfile#replace} moves the source over the target.
     *
     * @param dir a temporary directory for the files
     * @throws IOException if a file cannot be written
     */
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

    /**
     * Checks that a second thread never sees the target missing while {@link Flatfile#replace} runs many times.
     *
     * @param dir a temporary directory for the files
     * @throws Exception if a file cannot be written or the wait for the thread is interrupted
     */
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

    /**
     * Tests a configuration that is stored in one JSON file.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class SingleFile {

        /**
         * Checks that a write puts the configuration in its file as JSON.
         */
        @Test
        void writesTheConfigurationAsJson() {
            var config = StorageTestConfigs.single(new Flatfile());

            assertThat(await(config.write())).isTrue();

            assertThat(read(file())).contains("\"number\": 1", "\"text\": \"default\"");
        }

        /**
         * Checks that a read loads values that were changed in the file.
         *
         * @throws IOException if the file cannot be written
         */
        @Test
        void readsChangesFromDisk() throws IOException {
            var config = StorageTestConfigs.single(new Flatfile());
            Files.writeString(file(), "{\"number\": 7, \"text\": \"edited\"}");

            assertThat(await(config.read())).isTrue();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(7);
        }

        /**
         * Checks that a read of a missing file keeps the default values and creates the file.
         */
        @Test
        void readingAMissingFileKeepsTheDefaults() {
            var config = StorageTestConfigs.single(new Flatfile());

            assertThat(await(config.read(true))).as("nothing was read").isFalse();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(1);
            assertThat(file()).as("the file is created for the next write").exists();
        }

        /**
         * Checks that a corrupt file is renamed to {@code .invalid} and that the default values stay.
         *
         * @throws IOException if the file cannot be written
         */
        @Test
        void corruptFileIsMarkedInvalidAndDefaultsAreKept() throws IOException {
            var config = StorageTestConfigs.single(new Flatfile());
            Files.writeString(file(), "{\"number\": \"not a number\"}");

            assertThat(await(config.read())).isFalse();

            assertThat(StorageTestConfigs.number(config.get())).isEqualTo(1);
            assertThat(file()).doesNotExist();
            assertThat(read(file().resolveSibling("sampleconfiguration.json.invalid"))).contains("not a number");
        }

        /**
         * Checks that a second corrupt file gets a numbered {@code .invalid} copy and does not replace the first.
         *
         * @throws IOException if the file cannot be written
         */
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

        /**
         * @return the path of the configuration file in the mod directory
         */
        private static Path file() {
            return modDir().resolve("sampleconfiguration.json");
        }
    }

    /**
     * Tests a configuration that is stored in an AON file with comments.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class Aon {

        /**
         * Checks that a write puts the comments and the values in the file.
         */
        @Test
        void writesComments() {
            var config = StorageTestConfigs.aon(new Flatfile());

            await(config.write());

            assertThat(read(file())).contains("# Sample AON file", "# Second line", "# How many things", "count: 3");
        }

        /**
         * Checks that a read loads an edited value, and that the next write keeps the comments.
         *
         * @throws IOException if the file cannot be written
         */
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

        /**
         * Checks that a JSON file with the same name is read, then replaced by an AON file on the next write.
         *
         * @throws IOException if the file cannot be written
         */
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

        /**
         * @return the path of the AON file in the mod directory
         */
        private static Path file() {
            return modDir().resolve("commented.aon");
        }
    }

    /**
     * Tests data that is stored as one JSON file per instance in a directory.
     */
    @Nested
    @MinecraftBootstrapped
    @ExtendWith(AtlantisTestEnvironment.class)
    class DataFiles {

        /**
         * Checks that a specific write puts one instance in its own file and leaves no temporary file.
         *
         * @throws InterruptedException if the wait for the write is interrupted
         */
        @Test
        void writeSpecificWritesOneFilePerInstance() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(4);

            assertThat(await(config.storage().writeSpecific(config, data))).isTrue();

            assertThat(read(file(data.uuid().toString()))).contains("\"value\": 4");
            assertThat(dir().resolve(data.uuid() + ".json.temp")).doesNotExist();
        }

        /**
         * Checks that a full write puts every instance in its file.
         */
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

        /**
         * Checks that a full write with no instances completes with {@code true}.
         */
        @Test
        void writeWithNothingToWriteCompletes() {
            var config = StorageTestConfigs.data(new Flatfile());

            assertThat(await(config.write())).isTrue();
        }

        /**
         * Checks that a full read loads every file in the directory.
         *
         * @throws InterruptedException if the wait for a write is interrupted
         */
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

        /**
         * Checks that a read by UUID loads one file and puts the instance in the config.
         *
         * @throws InterruptedException if the wait for the write is interrupted
         */
        @Test
        void readFromUuidLoadsOneFile() throws InterruptedException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(3);
            await(config.storage().writeSpecific(config, data));

            var read = await(config.storage().readFromUUID(config, data.uuid()));

            assertThat(read).isEqualTo(data);
            assertThat(config.uuidDataMap()).containsKey(data.uuid());
        }

        /**
         * Checks that a read by UUID of a corrupt file gives {@code null} and renames the file to {@code .invalid}.
         *
         * @throws IOException if the file cannot be written
         */
        @Test
        void corruptFileReadFromUuidIsMarkedInvalid() throws IOException {
            var config = StorageTestConfigs.data(new Flatfile());
            var data = StorageTestConfigs.newData(3);
            Files.writeString(file(data.uuid().toString()), "{\"value\": \"oops\"}");

            assertThat(await(config.storage().readFromUUID(config, data.uuid()))).isNull();

            assertThat(file(data.uuid().toString())).doesNotExist();
            assertThat(dir().resolve(data.uuid() + ".json.invalid")).exists();
        }

        /**
         * Checks that a full read skips a corrupt file and leaves it in place.
         *
         * @throws IOException if the file cannot be written
         * @throws InterruptedException if the wait for the write is interrupted
         */
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

        /**
         * Checks that a remove moves the file of the instance to the {@code bin} directory of Atlantis.
         *
         * @throws InterruptedException if the wait for the write is interrupted
         */
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

        /**
         * @return the path of the data directory in the mod directory
         */
        private static Path dir() {
            return modDir().resolve("sampledata");
        }

        /**
         * Gets the path of a data file in the data directory.
         *
         * @param identifier the file name without the {@code .json} extension
         * @return the path of the file
         */
        private static Path file(String identifier) {
            return dir().resolve(identifier + ".json");
        }
    }
}
