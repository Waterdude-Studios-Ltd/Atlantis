package de.waterdu.atlantis.trident;

import com.google.gson.Gson;
import de.waterdu.atlantis.util.java.version.Version;
import de.waterdu.atlantis.util.java.version.VersionRange;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Holds the contents of the {@code tine.json} of a Tine.
 *
 * @param id the ID of the Tine. It may only contain {@code [a-z0-9_.-]}.
 * @param version the version of the Tine
 * @param name the display name of the Tine, or the ID if it is not set
 * @param authors the authors of the Tine
 * @param issues the URL of the issue tracker, or {@code null}
 * @param atlantis the range of Atlantis versions that the Tine supports, {@code *} if it is not set
 * @param order the load order of the Tine among Tines with no dependency between them
 * @param depends the version ranges of the Tines or loader mods that the Tine needs, by ID
 * @param mixins the names of the mixin configs of the Tine
 */
public record TineMetadata(
        String id,
        String version,
        String name,
        List<String> authors,
        @Nullable String issues,
        String atlantis,
        int order,
        Map<String, String> depends,
        List<String> mixins
) {

    public static final String FILE_NAME = "tine.json";

    private static final Gson GSON = new Gson();
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+");

    /**
     * Creates the metadata. It validates the ID, version and version ranges, and replaces missing values with
     * defaults.
     *
     * @param id the ID of the Tine
     * @param version the version of the Tine
     * @param name the display name of the Tine, or {@code null} to use the ID
     * @param authors the authors of the Tine, or {@code null} for none
     * @param issues the URL of the issue tracker, or {@code null}
     * @param atlantis the range of Atlantis versions that the Tine supports, or {@code null} for {@code *}
     * @param order the load order of the Tine
     * @param depends the version ranges of the needed Tines or loader mods by ID, or {@code null} for none
     * @param mixins the names of the mixin configs of the Tine, or {@code null} for none
     * @throws IllegalArgumentException if the ID, the version or a version range is not valid
     */
    public TineMetadata {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid Tine ID '" + id + "', IDs may only contain [a-z0-9_.-]");
        }

        if (version == null || Version.parse(version).isEmpty()) {
            throw new IllegalArgumentException("Invalid version '" + version + "' for Tine " + id);
        }

        name = name == null ? id : name;
        authors = authors == null ? List.of() : List.copyOf(authors);
        atlantis = atlantis == null ? "*" : atlantis;
        depends = depends == null ? Map.of() : Map.copyOf(depends);
        mixins = mixins == null ? List.of() : List.copyOf(mixins);

        VersionRange.parse(atlantis);
        depends.values().forEach(VersionRange::parse);
    }

    /**
     * Parses the metadata from the JSON contents of a {@code tine.json}.
     *
     * @param json the JSON contents
     * @return the metadata
     * @throws IllegalArgumentException if the JSON is empty or the metadata is not valid
     */
    public static TineMetadata parse(String json) {
        var metadata = GSON.fromJson(json, TineMetadata.class);
        if (metadata == null) {
            throw new IllegalArgumentException(FILE_NAME + " is empty");
        }
        return metadata;
    }

    /**
     * @return the parsed range of Atlantis versions that the Tine supports
     */
    public VersionRange atlantisRange() {
        return VersionRange.parse(this.atlantis);
    }
}
