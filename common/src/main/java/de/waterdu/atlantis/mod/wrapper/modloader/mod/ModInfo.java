package de.waterdu.atlantis.mod.wrapper.modloader.mod;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Describes a mod that the loader has loaded.
 *
 * @param id the mod ID
 * @param displayName the name of the mod to show to players
 * @param version the version of the mod
 * @param authors the authors of the mod
 * @param issueTracker the URL of the issue tracker, or {@code null} if the mod has none
 * @param versionCheck gets the result of the update check for the mod
 */
public record ModInfo(
        String id,
        String displayName,
        String version,
        List<String> authors,
        @Nullable String issueTracker,
        Supplier<VersionCheck> versionCheck
) {}
