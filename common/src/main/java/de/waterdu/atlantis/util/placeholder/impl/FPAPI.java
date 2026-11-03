package de.waterdu.atlantis.util.placeholder.impl;

import com.envyful.papi.api.PlaceholderFactory;
import com.envyful.papi.api.manager.AbstractPlaceholderManager;
import com.envyful.papi.api.manager.extensions.PlaceholderExtension;
import com.envyful.papi.api.util.UtilPlaceholder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides placeholders through FPAPI.
 */
public class FPAPI implements PlaceholderProvider {

    private final Map<Class<?>, Map<String, Impl<?>>> impl = new HashMap<>();

    /**
     * Creates the provider.
     */
    public FPAPI() {}

    /**
     * Adds the extension to the FPAPI manager for the ID and the type. If no such manager exists, it creates one and
     * registers it with {@link PlaceholderFactory}.
     *
     * @param <T> the type of the context object
     * @param id the ID of the manager
     * @param name the name of the manager
     * @param extension the extension to register
     * @param type the class of the context object
     */
    @SuppressWarnings("unchecked")
    public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        ((Impl<T>) this.impl.computeIfAbsent(type, t -> new HashMap<>()).computeIfAbsent(id, i -> {
            var newImpl = new Impl<>(id, name, type);
            PlaceholderFactory.register(newImpl);
            return newImpl;
        })).registerPlaceholder(new ExtensionImpl<>(extension));
    }

    /**
     * Replaces the placeholders in the text with {@link UtilPlaceholder}.
     *
     * @param <T> the type of the context object
     * @param object the context object that the placeholders read from
     * @param text the text that contains the placeholders
     * @return a new text with the placeholders replaced
     */
    @Override
    public <T> Text replace(T object, Text text) {
        return Text.of(UtilPlaceholder.replaceIdentifiers(object, text.get()));
    }

    /**
     * Replaces the placeholders in the text for a player with {@link UtilPlaceholder}.
     *
     * @param uuid the UUID of the player
     * @param text the text that contains the placeholders
     * @return a new text with the placeholders replaced
     */
    @Override
    public Text replace(UUID uuid, Text text) {
        return Text.of(UtilPlaceholder.replaceIdentifiers(uuid, text.get()));
    }

    /**
     * Adapts an {@link AtlantisPlaceholderExtension} to an FPAPI {@link PlaceholderExtension}.
     *
     * @param <T> the type of the context object
     * @param impl the extension to adapt
     */
    private record ExtensionImpl<T>(AtlantisPlaceholderExtension<T> impl) implements PlaceholderExtension<T> {

        /**
         * @return the name of the adapted extension
         */
        @Override
        public String getName() {
            return this.impl.name();
        }

        /**
         * @return the priority of the adapted extension
         */
        @Override
        public int getPriority() {
            return this.impl.priority();
        }

        /**
         * @return the description lines of the adapted extension
         */
        @Override
        public List<String> getDescription() {
            return this.impl.description();
        }

        /**
         * @return the example placeholders of the adapted extension
         */
        @Override
        public List<String> getExamples() {
            return this.impl.examples();
        }

        /**
         * Checks with the adapted extension if it can parse the placeholder.
         *
         * @param object the context object
         * @param placeholder the placeholder to check
         * @return {@code true} if the adapted extension can parse the placeholder
         */
        @Override
        public boolean matches(T object, String placeholder) {
            return this.impl.matches(object, placeholder);
        }

        /**
         * Checks with the adapted extension if it can parse the placeholder for a context object of any type.
         *
         * @param object the context object
         * @param placeholder the placeholder to check
         * @return {@code true} if the adapted extension can parse the placeholder
         */
        @Override
        public boolean matchesObject(Object object, String placeholder) {
            return this.impl.matchesObject(object, placeholder);
        }

        /**
         * Gets the value of the placeholder from the adapted extension.
         *
         * @param object the context object
         * @param placeholder the placeholder to parse
         * @return the value of the placeholder
         */
        @Override
        public String parse(T object, String placeholder) {
            return this.impl.parse(object, placeholder);
        }

        /**
         * Gets the value of the placeholder from the adapted extension for a context object of any type.
         *
         * @param object the context object
         * @param placeholder the placeholder to parse
         * @return the value of the placeholder, or {@code null} if the adapted extension cannot parse it
         */
        @Override
        public String parseObject(Object object, String placeholder) {
            return this.impl.parseObject(object, placeholder);
        }
    }

    /**
     * Holds the placeholders of one ID and type in FPAPI, with Atlantis as the author and version.
     *
     * @param <T> the type of the context object
     */
    private static class Impl<T> extends AbstractPlaceholderManager<T> {

        /**
         * Creates a manager.
         *
         * @param id the ID of the manager
         * @param name the name of the manager
         * @param type the class of the context object
         */
        public Impl(String id, String name, Class<T> type) {
            super(id, Atlantis.WATER_NAME_AS_ARRAY, Atlantis.VERSION, name, type);
        }
    }
}
