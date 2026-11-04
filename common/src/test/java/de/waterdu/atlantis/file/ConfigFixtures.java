package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.file.storage.Volatile;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Configuration classes for the config system tests, one per annotation and data type.
 */
public final class ConfigFixtures {

    public static final String MOD = "atlantis_config_test";

    /**
     * Prevents instances of this class.
     */
    private ConfigFixtures() {}

    /**
     * Holds a number and a text, as a basic configuration.
     */
    @BasicConfiguration(mod = MOD)
    public static class SampleConfiguration implements Configuration {

        public int number = 1;
        public String text = "default";
    }

    /**
     * Holds a UUID and a value, as basic data.
     */
    @BasicData(mod = MOD)
    public static class SampleData implements Data {

        private UUID uuid = UUID.randomUUID();
        public int value;

        /**
         * Creates data with a random UUID and a value of {@code 0}.
         */
        public SampleData() {}

        /**
         * Creates data with the given UUID and value.
         *
         * @param uuid the UUID of the data
         * @param value the value of the data
         */
        public SampleData(UUID uuid, int value) {
            this.uuid = uuid;
            this.value = value;
        }

        /**
         * @return the UUID of this data
         */
        @Override
        public UUID uuid() {
            return this.uuid;
        }

        /**
         * Sets the UUID of this data.
         *
         * @param uuid the new UUID
         */
        @Override
        public void setUUID(UUID uuid) {
            this.uuid = uuid;
        }

        /**
         * Gets the identifier of this data.
         *
         * @return the UUID as a string
         */
        @Override
        public String identifier() {
            return this.uuid.toString();
        }

        /**
         * Gets the hash code from {@link Data#defaultHashCode()}.
         *
         * @return the hash code of this data
         */
        @Override
        public int hashCode() {
            return this.defaultHashCode();
        }

        /**
         * Compares this data with {@link Data#defaultEquals(Object)}.
         *
         * @param obj the object to compare with
         * @return {@code true} if the object is equal to this data
         */
        @Override
        public boolean equals(Object obj) {
            return this.defaultEquals(obj);
        }

        /**
         * Gets the string from {@link Data#defaultToString()}.
         *
         * @return the string form of this data
         */
        @Override
        public String toString() {
            return this.defaultToString();
        }
    }

    /**
     * Adds a name to {@link SampleData}, stored in the {@code named} directory.
     */
    @FileBackedObject(mod = MOD, path = "named")
    public static class SampleNamedData extends SampleData implements NamedData {

        private String name = "";

        /**
         * Creates named data with a random UUID and an empty name.
         */
        public SampleNamedData() {}

        /**
         * Creates named data with the given UUID and name, and a value of {@code 0}.
         *
         * @param uuid the UUID of the data
         * @param name the name of the data
         */
        public SampleNamedData(UUID uuid, String name) {
            super(uuid, 0);
            this.name = name;
        }

        /**
         * @return the name of this data
         */
        @Override
        public String name() {
            return this.name;
        }

        /**
         * Sets the name of this data.
         *
         * @param name the new name
         */
        @Override
        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Acts as player data, stored in the {@code players} directory with {@link Volatile} storage.
     */
    @FileBackedObject(mod = MOD, path = "players", storage = Volatile.class)
    public static class SamplePlayerData extends SampleData implements PlayerData {

        /**
         * Creates player data with a random UUID.
         */
        public SamplePlayerData() {}

        /**
         * Creates player data for the given UUID.
         *
         * @param uuid the UUID of the player
         */
        public SamplePlayerData(UUID uuid) {
            super(uuid, 0);
        }

        /**
         * Gets the name of this data.
         *
         * @return the UUID as a string
         */
        @Override
        public String name() {
            return this.uuid().toString();
        }

        /**
         * Does nothing, because the name always comes from the UUID.
         *
         * @param name the name, which is ignored
         */
        @Override
        public void setName(String name) {}

        /**
         * Does nothing.
         *
         * @param player the player that logs in
         */
        @Override
        public void onLogin(ServerPlayer player) {}

        /**
         * Does nothing.
         *
         * @param player the player that logs out
         */
        @Override
        public void onLogout(ServerPlayer player) {}
    }

    /**
     * Holds a formatted string, a prefix and a message with an article and plural, as a lang file.
     */
    @I18nFile(mod = MOD)
    public static class SampleLang implements Configuration {

        public String greeting = "Hello %1";
        public String prefixPositive = "[+] ";
        public Message item = Message.of("sword").withArticle("a ").withPlural("s");
    }

    /**
     * Holds a count in an AON file with comments on the class and the field.
     */
    @FileBackedObject(mod = MOD, path = "commented.aon")
    @AonComment({"Sample AON file", "Second line"})
    public static class SampleAon implements Configuration {

        @AonComment("How many things")
        public int count = 3;
    }

    /**
     * Holds a value, and loads with {@link LoadOrder#FIRST}.
     */
    @FileBackedObject(mod = MOD, path = "first.json", order = LoadOrder.FIRST)
    public static class FirstConfiguration implements Configuration {

        public int value = 1;
    }

    /**
     * Holds a value, and loads with {@link LoadOrder#LAST}.
     */
    @FileBackedObject(mod = MOD, path = "last.json", order = LoadOrder.LAST)
    public static class LastConfiguration implements Configuration {

        public int value = 1;
    }

    /**
     * Holds a value with {@link Volatile} storage.
     */
    @FileBackedObject(mod = MOD, path = "volatile.json", storage = Volatile.class)
    public static class VolatileConfiguration implements Configuration {

        public int value = 1;
    }

    /**
     * Holds a value, and names a storage class that has no default constructor.
     */
    @FileBackedObject(mod = MOD, path = "fallback.json", storage = NoDefaultConstructorStorage.class)
    public static class FallbackStorageConfiguration implements Configuration {

        public int value = 1;
    }

    /**
     * Has a config annotation, but does not implement {@link Configuration}.
     */
    @BasicConfiguration(mod = MOD)
    public static class NotAConfiguration {}

    /**
     * Acts as a storage class that cannot be created with a default constructor.
     */
    public abstract static class NoDefaultConstructorStorage implements Storage {

        /**
         * Creates the storage.
         *
         * @param unused an argument that is not used
         */
        public NoDefaultConstructorStorage(String unused) {}
    }
}
