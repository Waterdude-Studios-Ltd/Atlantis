package de.waterdu.atlantis.util.java.version;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public record Version(int major, int minor, int patch, List<String> preRelease) implements Comparable<Version> {

    private static final Pattern PATTERN = Pattern.compile("v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?");
    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    public Version {
        preRelease = List.copyOf(preRelease);
    }

    public Version(int major, int minor, int patch) {
        this(major, minor, patch, List.of());
    }

    public static Optional<Version> parse(String version) {
        var matcher = PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Version(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                    matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)),
                    matcher.group(4) == null ? List.of() : List.of(matcher.group(4).split("\\."))
            ));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public int compareTo(Version other) {
        var result = Integer.compare(this.major, other.major);
        if (result == 0) {
            result = Integer.compare(this.minor, other.minor);
        }
        if (result == 0) {
            result = Integer.compare(this.patch, other.patch);
        }
        if (result != 0) {
            return result;
        }
        if (this.preRelease.isEmpty() || other.preRelease.isEmpty()) {
            return Boolean.compare(this.preRelease.isEmpty(), other.preRelease.isEmpty());
        }

        for (var i = 0; i < Math.min(this.preRelease.size(), other.preRelease.size()); i++) {
            result = Version.compareIdentifiers(this.preRelease.get(i), other.preRelease.get(i));
            if (result != 0) {
                return result;
            }
        }

        return Integer.compare(this.preRelease.size(), other.preRelease.size());
    }

    private static int compareIdentifiers(String first, String second) {
        var firstNumeric = NUMERIC.matcher(first).matches();
        var secondNumeric = NUMERIC.matcher(second).matches();
        if (firstNumeric && secondNumeric) {
            var firstDigits = first.replaceFirst("^0+(?=.)", "");
            var secondDigits = second.replaceFirst("^0+(?=.)", "");
            var result = Integer.compare(firstDigits.length(), secondDigits.length());
            return result != 0 ? result : firstDigits.compareTo(secondDigits);
        }

        if (firstNumeric || secondNumeric) {
            return firstNumeric ? -1 : 1;
        }

        return first.compareTo(second);
    }

    @Override
    public @NonNull String toString() {
        var version = this.major + "." + this.minor + "." + this.patch;
        return this.preRelease.isEmpty() ? version : version + "-" + String.join(".", this.preRelease);
    }
}
