package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public record TineContainer(TineMetadata metadata, List<Path> paths, @Nullable String mainClass) {

    public TineContainer {
        paths = List.copyOf(paths);
    }

    public String id() {
        return this.metadata.id();
    }

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
