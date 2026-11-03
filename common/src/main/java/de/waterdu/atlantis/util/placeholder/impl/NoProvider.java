package de.waterdu.atlantis.util.placeholder.impl;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.UUID;

/**
 * Provides no placeholders. Atlantis uses it when no placeholder mod is present.
 */
public final class NoProvider implements PlaceholderProvider {

    public static final PlaceholderProvider INSTANCE = new NoProvider();

    /**
     * Logs the attempt at debug level and registers nothing.
     *
     * @param <T> the type of the context object
     * @param id the ID of the placeholder group
     * @param name the name of the placeholder group
     * @param extension the extension to register
     * @param type the class of the context object
     */
    @Override
    public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        AtlantisLogger.debug("Attempt to register AtlantisPlaceholderExtension '{}' funneled into NoProvider", extension.name());
    }

    /**
     * Gets the text without changes.
     *
     * @param <T> the type of the context object
     * @param object the context object
     * @param text the text
     * @return the same text
     */
    @Override
    public <T> Text replace(T object, Text text) {
        return text;
    }

    /**
     * Gets the text without changes.
     *
     * @param uuid the UUID of the player
     * @param text the text
     * @return the same text
     */
    @Override
    public Text replace(UUID uuid, Text text) {
        return text;
    }
}
