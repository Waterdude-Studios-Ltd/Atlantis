package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;

public abstract class ConfigEventPayload implements EventPayload {

    private final Config<? extends Configuration> config;

    protected ConfigEventPayload(Config<? extends Configuration> config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public <T extends Configuration> Config<T> config() {
        return (Config<T>) this.config;
    }

    public static class Register extends ConfigEventPayload {

        private final String modId;
        private String path;
        private final ClassHolder<? extends Configuration> classes;
        private final Configuration instance;
        private final Storage storage;

        public Register(Config<? extends Configuration> config, String path) {
            super(config);
            this.modId = config.owner();
            this.path = path;
            this.classes = config.classes();
            this.instance = config.get();
            this.storage = config.storage();
        }

        @Override
        public Key<?> type() {
            return UEvent.REGISTER_CONFIG;
        }

        public String modId() {
            return this.modId;
        }

        public String path() {
            return this.path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> ClassHolder<T> classes() {
            return (ClassHolder<T>) this.classes;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T instance() {
            return (T) this.instance;
        }

        public Storage storage() {
            return this.storage;
        }
    }

    public static class Read extends ConfigEventPayload {

        private final Configuration object;

        public Read(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        @Override
        public Key<?> type() {
            return UEvent.READ_CONFIG;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }

    public static class Write extends ConfigEventPayload {

        private final Configuration object;

        public Write(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        @Override
        public Key<?> type() {
            return UEvent.WRITE_CONFIG;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }

    public static class Delete extends ConfigEventPayload {

        private final Configuration object;

        public Delete(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        @Override
        public Key<?> type() {
            return UEvent.DELETE_CONFIG;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }
}
