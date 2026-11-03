package de.waterdu.atlantis.util.java.version;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Holds a semantic version with a major, minor and patch number and optional pre-release identifiers.
 *
 * @param major the major number
 * @param minor the minor number
 * @param patch the patch number
 * @param preRelease the pre-release identifiers, or an empty list for a release
 */
public record Version(int major, int minor, int patch, List<String> preRelease) implements Comparable<Version> {

    private static final Pattern PATTERN = Pattern.compile("v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?");
    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    /**
     * Creates a version with an immutable copy of the pre-release identifiers.
     *
     * @param major the major number
     * @param minor the minor number
     * @param patch the patch number
     * @param preRelease the pre-release identifiers, or an empty list for a release
     */
    public Version {
        preRelease = List.copyOf(preRelease);
    }

    /**
     * Creates a release version with no pre-release identifiers.
     *
     * @param major the major number
     * @param minor the minor number
     * @param patch the patch number
     */
    public Version(int major, int minor, int patch) {
        this(major, minor, patch, List.of());
    }

    /**
     * Parses versions such as {@code 1}, {@code v1.2} or {@code 1.2.3-beta.1+build}. A missing minor or patch value is
     * 0. Build metadata is ignored.
     *
     * @return an empty {@link Optional} if the text is not a valid version
     */
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

    /**
     * Compares the major, minor and patch numbers in order. A release is greater than a pre-release of the same
     * numbers. Pre-release identifiers compare one by one, and a longer list is greater if all shared identifiers are
     * equal.
     *
     * @param other the version to compare with
     * @return a negative number, zero or a positive number if this version is less than, equal to or greater than
     *     {@code other}
     */
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

    /**
     * Compares two pre-release identifiers. Numeric identifiers compare by value and are less than other identifiers.
     * Other identifiers compare as text.
     *
     * @param first the first identifier
     * @param second the second identifier
     * @return a negative number, zero or a positive number if {@code first} is less than, equal to or greater than
     *     {@code second}
     */
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

    /**
     * @return the version as {@code major.minor.patch}, followed by {@code -} and the pre-release identifiers joined
     *     by {@code .} if there are any
     */
    @Override
    public @NonNull String toString() {
        var version = this.major + "." + this.minor + "." + this.patch;
        return this.preRelease.isEmpty() ? version : version + "-" + String.join(".", this.preRelease);
    }
}
