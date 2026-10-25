package de.waterdu.atlantis.trident;

import com.google.gson.Gson;
import de.waterdu.atlantis.util.java.version.Version;
import de.waterdu.atlantis.util.java.version.VersionRange;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public static TineMetadata parse(String json) {
        var metadata = GSON.fromJson(json, TineMetadata.class);
        if (metadata == null) {
            throw new IllegalArgumentException(FILE_NAME + " is empty");
        }
        return metadata;
    }

    public VersionRange atlantisRange() {
        return VersionRange.parse(this.atlantis);
    }
}
