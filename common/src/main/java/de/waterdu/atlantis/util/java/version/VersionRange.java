package de.waterdu.atlantis.util.java.version;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of version constraints, for example {@code >=1.2.0 <2.0.0 || ^3.1.0}. All space-separated constraints must
 * match. {@code ||} separates alternatives. The range supports {@code *}, {@code =}, {@code >}, {@code >=}, {@code <},
 * {@code <=}, {@code ~} (same minor, or same major if only a major is given) and {@code ^} (same major, or same
 * leftmost non-zero component). A bare version requires an exact match.
 */
public record VersionRange(List<List<Constraint>> alternatives) {

    public static final VersionRange ANY = new VersionRange(List.of(List.of()));

    /**
     * Creates a range with immutable copies of the alternatives.
     *
     * @param alternatives the alternatives, each a list of constraints that must all match
     */
    public VersionRange {
        alternatives = alternatives.stream().map(List::copyOf).toList();
    }

    /**
     * An empty range matches any version. Throws {@link IllegalArgumentException} if a version in the range is not
     * valid.
     */
    public static VersionRange parse(String range) {
        var alternatives = new ArrayList<List<Constraint>>();
        for (var alternative : range.split("\\|\\|")) {
            var constraints = new ArrayList<Constraint>();
            for (var part : alternative.trim().split("\\s+")) {
                if (!part.isEmpty()) {
                    VersionRange.parseConstraint(part, constraints);
                }
            }
            alternatives.add(constraints);
        }
        return new VersionRange(alternatives);
    }

    /**
     * Parses one constraint and adds the result to the list. {@code *} and {@code x} add nothing. {@code ~} and
     * {@code ^} add a lower and an upper bound.
     *
     * @param part the constraint text, without spaces
     * @param constraints the list that gets the parsed constraints
     * @throws IllegalArgumentException if the version in the constraint is not valid
     */
    private static void parseConstraint(String part, List<Constraint> constraints) {
        if (part.equals("*") || part.equalsIgnoreCase("x")) {
            return;
        }

        var prefix = part.startsWith(">=") || part.startsWith("<=") ? part.substring(0, 2)
                : part.startsWith(">") || part.startsWith("<") || part.startsWith("=") || part.startsWith("~") || part.startsWith("^") ? part.substring(0, 1)
                : "";
        var text = part.substring(prefix.length());
        var version = Version.parse(text).orElseThrow(() -> new IllegalArgumentException("Invalid version '" + text + "' in range '" + part + "'"));
        var components = text.replaceFirst("^v", "").split("[-+]", 2)[0].split("\\.").length;

        switch (prefix) {
            case ">=" -> constraints.add(new Constraint(Comparison.GREATER_OR_EQUAL, version));
            case "<=" -> constraints.add(new Constraint(Comparison.LESS_OR_EQUAL, version));
            case ">" -> constraints.add(new Constraint(Comparison.GREATER, version));
            case "<" -> constraints.add(new Constraint(Comparison.LESS, version));
            case "~" -> {
                constraints.add(new Constraint(Comparison.GREATER_OR_EQUAL, version));
                constraints.add(new Constraint(Comparison.LESS, components == 1
                        ? new Version(version.major() + 1, 0, 0)
                        : new Version(version.major(), version.minor() + 1, 0)));
            }
            case "^" -> {
                constraints.add(new Constraint(Comparison.GREATER_OR_EQUAL, version));
                constraints.add(new Constraint(Comparison.LESS, version.major() > 0 || components == 1
                        ? new Version(version.major() + 1, 0, 0)
                        : version.minor() > 0 || components == 2
                        ? new Version(0, version.minor() + 1, 0)
                        : new Version(0, 0, version.patch() + 1)));
            }
            default -> constraints.add(new Constraint(Comparison.EQUAL, version));
        }
    }

    /**
     * @return {@code true} if an alternative has no constraints, so that every version matches
     */
    public boolean isAny() {
        return this.alternatives.stream().anyMatch(List::isEmpty);
    }

    /**
     * Checks if the version matches all constraints of at least one alternative.
     *
     * @param version the version to check
     * @return {@code true} if the version is in the range
     */
    public boolean test(Version version) {
        return this.alternatives.stream().anyMatch(constraints -> constraints.stream().allMatch(constraint -> constraint.test(version)));
    }

    /**
     * @return {@code false} if the text is not a valid version, unless the range matches any version
     */
    public boolean test(String version) {
        return this.isAny() || Version.parse(version).map(this::test).orElse(false);
    }

    /**
     * Compares a version with a fixed version.
     *
     * @param comparison the comparison to use
     * @param version the version to compare with
     */
    public record Constraint(Comparison comparison, Version version) {

        /**
         * Checks if the candidate compares to {@link #version()} as {@link #comparison()} requires.
         *
         * @param candidate the version to check
         * @return {@code true} if the candidate matches the constraint
         */
        public boolean test(Version candidate) {
            var result = candidate.compareTo(this.version);
            return switch (this.comparison) {
                case EQUAL -> result == 0;
                case GREATER -> result > 0;
                case GREATER_OR_EQUAL -> result >= 0;
                case LESS -> result < 0;
                case LESS_OR_EQUAL -> result <= 0;
            };
        }
    }

    /**
     * Lists the comparisons that a {@link Constraint} can use.
     */
    public enum Comparison {

        EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL,
    }
}
