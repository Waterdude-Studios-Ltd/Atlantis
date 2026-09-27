package de.waterdu.atlantis.util.placeholder.impl;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.UUID;

public final class NoProvider implements PlaceholderProvider {

    public static final PlaceholderProvider INSTANCE = new NoProvider();

    @Override
    public <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type) {
        AtlantisLogger.debug("Attempt to register AtlantisPlaceholderExtension '{}' funneled into NoProvider", extension.name());
    }

    @Override
    public <T> Text replace(T object, Text text) {
        return text;
    }

    @Override
    public Text replace(UUID uuid, Text text) {
        return text;
    }
}
