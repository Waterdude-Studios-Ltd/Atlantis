package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Holds a discovered Tine: its metadata, the paths it loads from and its main class.
 *
 * @param metadata the metadata from the {@code tine.json} of the Tine
 * @param paths the jar or directories of the Tine
 * @param mainClass the name of the {@link Tine} class, or {@code null} if the Tine has none
 */
public record TineContainer(TineMetadata metadata, List<Path> paths, @Nullable String mainClass) {

    /**
     * Creates the container with an immutable copy of the paths.
     *
     * @param metadata the metadata from the {@code tine.json} of the Tine
     * @param paths the jar or directories of the Tine
     * @param mainClass the name of the {@link Tine} class, or {@code null} if the Tine has none
     */
    public TineContainer {
        paths = List.copyOf(paths);
    }

    /**
     * @return the ID of the Tine
     */
    public String id() {
        return this.metadata.id();
    }

    /**
     * Creates the mod info of the Tine. Tines do not support version checks.
     *
     * @return the mod info with the ID, name, version, authors and issues URL of the Tine
     */
    public ModInfo toModInfo() {
        return new ModInfo(
                this.metadata.id(),
                this.metadata.name(),
                this.metadata.version(),
                this.metadata.authors(),
                this.metadata.issues(),
                () -> VersionCheck.UNSUPPORTED
        );
    }
}
