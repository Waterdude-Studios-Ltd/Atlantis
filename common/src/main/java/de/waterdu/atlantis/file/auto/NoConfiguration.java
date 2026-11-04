package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.datatypes.Configuration;

/**
 * Marks that no configuration class is set, for example in {@link FileBackedObject#providerClass()}.
 */
public final class NoConfiguration implements Configuration {}
