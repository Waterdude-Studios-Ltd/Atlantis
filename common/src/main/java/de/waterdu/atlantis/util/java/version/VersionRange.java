package de.waterdu.atlantis.util.java.version;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of version constraints, e.g. {@code >=1.2.0 <2.0.0 || ^3.1.0}. Space-separated constraints must all match and
 * {@code ||} separates alternatives. Supports {@code *}, {@code =}, {@code >}, {@code >=}, {@code <}, {@code <=},
 * {@code ~} (same minor, or same major if only a major is given) and {@code ^} (same major, or same leftmost non-zero
 * component). A bare version requires an exact match.
 */
public record VersionRange(List<List<Constraint>> alternatives) {

    public static final VersionRange ANY = new VersionRange(List.of(List.of()));

    public VersionRange {
        alternatives = alternatives.stream().map(List::copyOf).toList();
    }

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

    public boolean isAny() {
        return this.alternatives.stream().anyMatch(List::isEmpty);
    }

    public boolean test(Version version) {
        return this.alternatives.stream().anyMatch(constraints -> constraints.stream().allMatch(constraint -> constraint.test(version)));
    }

    public boolean test(String version) {
        return this.isAny() || Version.parse(version).map(this::test).orElse(false);
    }

    public record Constraint(Comparison comparison, Version version) {

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

    public enum Comparison {

        EQUAL,
        GREATER,
        GREATER_OR_EQUAL,
        LESS,
        LESS_OR_EQUAL,
    }
}
