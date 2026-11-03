package de.waterdu.atlantis.util.placeholder.impl;

import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.UUID;

/**
 * Connects Atlantis placeholders to a placeholder mod.
 */
public interface PlaceholderProvider {

    /**
     * Registers the extension with the placeholder mod.
     *
     * @param <T> the type of the context object
     * @param id the ID of the placeholder group
     * @param name the name of the placeholder group
     * @param extension the extension to register
     * @param type the class of the context object
     */
    <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type);

    /**
     * Replaces the placeholders in the text.
     *
     * @param <T> the type of the context object
     * @param object the context object that the placeholders read from
     * @param text the text that contains the placeholders
     * @return the text with the placeholders replaced
     */
    <T> Text replace(T object, Text text);

    /**
     * Replaces the placeholders in the text for a player.
     *
     * @param uuid the UUID of the player
     * @param text the text that contains the placeholders
     * @return the text with the placeholders replaced
     */
    Text replace(UUID uuid, Text text);
}
