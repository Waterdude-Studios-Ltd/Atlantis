package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.ModExtension;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record RegisterModExtensionsEventPayload(Set<ModExtension> extensions) implements EventPayload {

    public RegisterModExtensionsEventPayload() {
        this(new HashSet<>());
    }

    @Override
    public Key<?> type() {
        return UEvent.REGISTER_MOD_EXTENSIONS;
    }

    public void register(ModExtension extension) {
        this.extensions.add(extension);
    }

    public void applyExtensions(Map<ModExtension.Priority, Map<String, ModExtension>> extensions) {
        for (var extension : this.extensions) {
            extensions.get(extension.priority()).put(extension.modId(), extension);
        }
    }
}
