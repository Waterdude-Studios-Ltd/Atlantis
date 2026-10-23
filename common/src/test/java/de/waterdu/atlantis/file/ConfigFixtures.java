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

    private ConfigFixtures() {}

    @BasicConfiguration(mod = MOD)
    public static class SampleConfiguration implements Configuration {

        public int number = 1;
        public String text = "default";
    }

    @BasicData(mod = MOD)
    public static class SampleData implements Data {

        private UUID uuid = UUID.randomUUID();
        public int value;

        public SampleData() {}

        public SampleData(UUID uuid, int value) {
            this.uuid = uuid;
            this.value = value;
        }

        @Override
        public UUID uuid() {
            return this.uuid;
        }

        @Override
        public void setUUID(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public String identifier() {
            return this.uuid.toString();
        }

        @Override
        public int hashCode() {
            return this.defaultHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this.defaultEquals(obj);
        }

        @Override
        public String toString() {
            return this.defaultToString();
        }
    }

    @FileBackedObject(mod = MOD, path = "named")
    public static class SampleNamedData extends SampleData implements NamedData {

        private String name = "";

        public SampleNamedData() {}

        public SampleNamedData(UUID uuid, String name) {
            super(uuid, 0);
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }
    }

    @FileBackedObject(mod = MOD, path = "players", storage = Volatile.class)
    public static class SamplePlayerData extends SampleData implements PlayerData {

        public SamplePlayerData() {}

        public SamplePlayerData(UUID uuid) {
            super(uuid, 0);
        }

        @Override
        public String name() {
            return this.uuid().toString();
        }

        @Override
        public void setName(String name) {}

        @Override
        public void onLogin(ServerPlayer player) {}

        @Override
        public void onLogout(ServerPlayer player) {}
    }

    @I18nFile(mod = MOD)
    public static class SampleLang implements Configuration {

        public String greeting = "Hello %1";
        public String prefixPositive = "[+] ";
        public Message item = Message.of("sword").withArticle("a ").withPlural("s");
    }

    @FileBackedObject(mod = MOD, path = "commented.aon")
    @AonComment({"Sample AON file", "Second line"})
    public static class SampleAon implements Configuration {

        @AonComment("How many things")
        public int count = 3;
    }

    @FileBackedObject(mod = MOD, path = "first.json", order = LoadOrder.FIRST)
    public static class FirstConfiguration implements Configuration {

        public int value = 1;
    }

    @FileBackedObject(mod = MOD, path = "last.json", order = LoadOrder.LAST)
    public static class LastConfiguration implements Configuration {

        public int value = 1;
    }

    @FileBackedObject(mod = MOD, path = "volatile.json", storage = Volatile.class)
    public static class VolatileConfiguration implements Configuration {

        public int value = 1;
    }

    @FileBackedObject(mod = MOD, path = "fallback.json", storage = NoDefaultConstructorStorage.class)
    public static class FallbackStorageConfiguration implements Configuration {

        public int value = 1;
    }

    @BasicConfiguration(mod = MOD)
    public static class NotAConfiguration {}

    public abstract static class NoDefaultConstructorStorage implements Storage {

        public NoDefaultConstructorStorage(String unused) {}
    }
}
