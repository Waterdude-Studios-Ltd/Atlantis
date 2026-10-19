package de.waterdu.atlantis.mod.wrapper.modloader.mod;

import org.jspecify.annotations.Nullable;

public record VersionCheck(VersionStatus status, @Nullable String target) {

    public static final VersionCheck UNSUPPORTED = new VersionCheck(VersionStatus.UNSUPPORTED, null);
}
