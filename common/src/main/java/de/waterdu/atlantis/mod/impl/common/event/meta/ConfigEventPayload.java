package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;

/**
 * Holds the config of a config event.
 */
public abstract class ConfigEventPayload implements EventPayload {

    private final Config<? extends Configuration> config;

    /**
     * Creates a config payload.
     *
     * @param config the config of the event
     */
    protected ConfigEventPayload(Config<? extends Configuration> config) {
        this.config = config;
    }

    /**
     * Gets the config of the event, cast to the type that the caller expects.
     *
     * @param <T> the configuration type
     * @return the config
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> Config<T> config() {
        return (Config<T>) this.config;
    }

    /**
     * Fires when a config is registered, before it initializes. Listeners can change the path of the config.
     */
    public static class Register extends ConfigEventPayload {

        private final String modId;
        private String path;
        private final ClassHolder<? extends Configuration> classes;
        private final Configuration instance;
        private final Storage storage;

        /**
         * Creates a payload for {@link UEvent#REGISTER_CONFIG}. Copies the owner, classes, instance and storage from
         * the config.
         *
         * @param config the config that is registered
         * @param path the path that the config initializes with
         */
        public Register(Config<? extends Configuration> config, String path) {
            super(config);
            this.modId = config.owner();
            this.path = path;
            this.classes = config.classes();
            this.instance = config.get();
            this.storage = config.storage();
        }

        /**
         * @return {@link UEvent#REGISTER_CONFIG}
         */
        @Override
        public Key<?> type() {
            return UEvent.REGISTER_CONFIG;
        }

        /**
         * @return the ID of the mod that owns the config
         */
        public String modId() {
            return this.modId;
        }

        /**
         * @return the path that the config initializes with
         */
        public String path() {
            return this.path;
        }

        /**
         * Sets the path that the config initializes with.
         *
         * @param path the new path
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Gets the class holder of the config, cast to the type that the caller expects.
         *
         * @param <T> the configuration type
         * @return the class holder
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> ClassHolder<T> classes() {
            return (ClassHolder<T>) this.classes;
        }

        /**
         * Gets the configuration instance of the config at registration, cast to the type that the caller expects.
         *
         * @param <T> the configuration type
         * @return the configuration instance
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> T instance() {
            return (T) this.instance;
        }

        /**
         * @return the storage backend of the config
         */
        public Storage storage() {
            return this.storage;
        }
    }

    /**
     * Fires after a config or one of its instances is read from storage.
     */
    public static class Read extends ConfigEventPayload {

        private final Configuration object;

        /**
         * Creates a payload for {@link UEvent#READ_CONFIG}.
         *
         * @param config the config that was read
         * @param object the object that was read, or {@code null}
         */
        public Read(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        /**
         * @return {@link UEvent#READ_CONFIG}
         */
        @Override
        public Key<?> type() {
            return UEvent.READ_CONFIG;
        }

        /**
         * The object that was read. It can be null, for example when the read fails.
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }

    /**
     * Fires after a config or one of its instances is written to storage.
     */
    public static class Write extends ConfigEventPayload {

        private final Configuration object;

        /**
         * Creates a payload for {@link UEvent#WRITE_CONFIG}.
         *
         * @param config the config that was written
         * @param object the object that was written
         */
        public Write(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        /**
         * @return {@link UEvent#WRITE_CONFIG}
         */
        @Override
        public Key<?> type() {
            return UEvent.WRITE_CONFIG;
        }

        /**
         * Gets the object that was written, cast to the type that the caller expects.
         *
         * @param <T> the configuration type
         * @return the written object
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }

    /**
     * Fires after an instance of a config is deleted from storage.
     */
    public static class Delete extends ConfigEventPayload {

        private final Configuration object;

        /**
         * Creates a payload for {@link UEvent#DELETE_CONFIG}.
         *
         * @param config the config of the deleted instance
         * @param object the object that was deleted, or {@code null}
         */
        public Delete(Config<? extends Configuration> config, Configuration object) {
            super(config);
            this.object = object;
        }

        /**
         * @return {@link UEvent#DELETE_CONFIG}
         */
        @Override
        public Key<?> type() {
            return UEvent.DELETE_CONFIG;
        }

        /**
         * The object that was deleted. It can be null, for example when the delete fails.
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> T object() {
            return (T) this.object;
        }
    }
}
