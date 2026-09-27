package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;

public class ConfigurationContainer<T extends Configuration> {

    private volatile T configuration;

    private final ResettableLazy<Boolean> isData = ResettableLazy.of(() -> this.get() instanceof Data);
    private final ResettableLazy<Boolean> isNamedData = ResettableLazy.of(() -> this.get() instanceof NamedData);
    private final ResettableLazy<Boolean> isPlayerData = ResettableLazy.of(() -> this.get() instanceof PlayerData);

    public void set(Config<T> config, T configuration) {
        if (!config.onPut(configuration)) {
            this.isData.reset();
            this.isNamedData.reset();
            this.isPlayerData.reset();
            this.configuration = configuration;
        }
    }

    public T get() {
        return this.configuration;
    }

    public boolean isData() {
        return this.isData.get();
    }

    public boolean isNamedData() {
        return this.isNamedData.get();
    }

    public boolean isPlayerData() {
        return this.isPlayerData.get();
    }
}
