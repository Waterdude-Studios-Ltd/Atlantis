package de.waterdu.atlantis.mod.wrapper.modloader.mod;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public record ModInfo(
        String id,
        String displayName,
        String version,
        List<String> authors,
        @Nullable String issueTracker,
        Supplier<VersionCheck> versionCheck
) {}
