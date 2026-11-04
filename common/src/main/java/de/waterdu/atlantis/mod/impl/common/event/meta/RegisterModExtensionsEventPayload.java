package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.ModExtension;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects mod extensions from listeners.
 *
 * @param extensions the registered extensions
 */
public record RegisterModExtensionsEventPayload(Set<ModExtension> extensions) implements EventPayload {

    /**
     * Creates a payload with an empty set of extensions.
     */
    public RegisterModExtensionsEventPayload() {
        this(new HashSet<>());
    }

    /**
     * @return {@link UEvent#REGISTER_MOD_EXTENSIONS}
     */
    @Override
    public Key<?> type() {
        return UEvent.REGISTER_MOD_EXTENSIONS;
    }

    /**
     * Adds an extension. It replaces a registered extension with the same mod ID and priority. Extensions with
     * {@link ModExtension.Priority#HIGHEST} do not activate, because the event fires after that priority activates.
     */
    public void register(ModExtension extension) {
        this.extensions.add(extension);
    }

    /**
     * Puts each registered extension into the map of its priority, keyed by its mod ID.
     *
     * @param extensions the extension maps by priority
     */
    public void applyExtensions(Map<ModExtension.Priority, Map<String, ModExtension>> extensions) {
        for (var extension : this.extensions) {
            extensions.get(extension.priority()).put(extension.modId(), extension);
        }
    }
}
