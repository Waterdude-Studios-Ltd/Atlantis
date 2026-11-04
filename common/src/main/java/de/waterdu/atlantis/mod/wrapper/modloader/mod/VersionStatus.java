package de.waterdu.atlantis.mod.wrapper.modloader.mod;

/**
 * Tells how the installed version of a mod compares to the version that an update check found.
 */
public enum VersionStatus {

    PENDING,
    FAILED,
    UP_TO_DATE,
    OUTDATED,
    AHEAD,
    BETA,
    BETA_OUTDATED,
    UNSUPPORTED,
}
