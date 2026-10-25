package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.VersionCheck;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public record TineContainer(TineMetadata metadata, Path path, @Nullable String mainClass) {

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
