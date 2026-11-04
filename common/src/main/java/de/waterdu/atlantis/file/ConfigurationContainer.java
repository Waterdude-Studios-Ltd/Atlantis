package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;

/**
 * Holds the configuration instance of a {@link Config} and caches which data type it is.
 *
 * @param <T> the configuration type
 */
public class ConfigurationContainer<T extends Configuration> {

    private volatile T configuration;

    private final ResettableLazy<Boolean> isData = ResettableLazy.of(() -> this.get() instanceof Data);
    private final ResettableLazy<Boolean> isNamedData = ResettableLazy.of(() -> this.get() instanceof NamedData);
    private final ResettableLazy<Boolean> isPlayerData = ResettableLazy.of(() -> this.get() instanceof PlayerData);

    /**
     * Does nothing if a put listener stops the change. See {@link Config#addPutListener(java.util.function.Predicate)}.
     */
    public void set(Config<T> config, T configuration) {
        if (!config.onPut(configuration)) {
            this.isData.reset();
            this.isNamedData.reset();
            this.isPlayerData.reset();
            this.configuration = configuration;
        }
    }

    /**
     * @return the configuration instance
     */
    public T get() {
        return this.configuration;
    }

    /**
     * @return {@code true} if the configuration implements {@link Data}
     */
    public boolean isData() {
        return this.isData.get();
    }

    /**
     * @return {@code true} if the configuration implements {@link NamedData}
     */
    public boolean isNamedData() {
        return this.isNamedData.get();
    }

    /**
     * @return {@code true} if the configuration implements {@link PlayerData}
     */
    public boolean isPlayerData() {
        return this.isPlayerData.get();
    }
}
