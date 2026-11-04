package de.waterdu.atlantis.mod.wrapper.modloader.mod;

import org.jspecify.annotations.Nullable;

/**
 * Holds the result of an update check for a mod.
 *
 * @param status the result of the check
 * @param target the version that the check found, or {@code null} if there is none
 */
public record VersionCheck(VersionStatus status, @Nullable String target) {

    public static final VersionCheck UNSUPPORTED = new VersionCheck(VersionStatus.UNSUPPORTED, null);
}
