package de.waterdu.atlantis.util.placeholder.impl;

import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderExtension;
import de.waterdu.atlantis.util.text.Text;

import java.util.UUID;

public interface PlaceholderProvider {

    <T> void register(String id, String name, AtlantisPlaceholderExtension<T> extension, Class<T> type);

    <T> Text replace(T object, Text text);

    Text replace(UUID uuid, Text text);
}
