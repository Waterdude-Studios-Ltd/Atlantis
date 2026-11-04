package de.waterdu.atlantis.util.java.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link Version} and {@link VersionRange}.
 */
class VersionRangeTest {

    /**
     * Checks that {@link Version#parse} accepts short versions, a {@code v} prefix, build metadata and pre-release
     * parts, and rejects text and versions with more than three numbers.
     */
    @Test
    void parsesLenientVersions() {
        assertThat(Version.parse("1.2")).contains(new Version(1, 2, 0));
        assertThat(Version.parse("v3")).contains(new Version(3, 0, 0));
        assertThat(Version.parse("0.116.6+1.21.1")).contains(new Version(0, 116, 6));
        assertThat(Version.parse("1.0.0-beta.2")).contains(new Version(1, 0, 0, List.of("beta", "2")));
        assertThat(Version.parse("not a version")).isEmpty();
        assertThat(Version.parse("1.2.3.4")).isEmpty();
    }

    /**
     * Checks that pre-releases sort before the release, and that numeric pre-release parts sort as numbers.
     */
    @Test
    void ordersPreReleasesBeforeReleases() {
        assertThat(Version.parse("1.0.0-alpha").orElseThrow()).isLessThan(Version.parse("1.0.0-alpha.1").orElseThrow());
        assertThat(Version.parse("1.0.0-alpha.1").orElseThrow()).isLessThan(Version.parse("1.0.0-beta").orElseThrow());
        assertThat(Version.parse("1.0.0-beta.2").orElseThrow()).isLessThan(Version.parse("1.0.0-beta.11").orElseThrow());
        assertThat(Version.parse("1.0.0-rc.1").orElseThrow()).isLessThan(Version.parse("1.0.0").orElseThrow());
    }

    /**
     * Checks that a range matches a version as expected. Covers wildcards, comparisons, tilde and caret ranges,
     * ranges joined by spaces and alternatives joined by {@code ||}.
     *
     * @param range the range to parse
     * @param version the version to test
     * @param matches {@code true} if the range must match the version
     */
    @ParameterizedTest
    @CsvSource({
            "*, 0.0.1, true",
            "'', 5.0.0, true",
            "1.2.3, 1.2.3, true",
            "1.2.3, 1.2.4, false",
            "=1.2.3, 1.2.3, true",
            ">1.2.3, 1.2.4, true",
            ">1.2.3, 1.2.3, false",
            ">=1.2.3, 1.2.3, true",
            "<2.0.0, 1.9.9, true",
            "<2.0.0, 2.0.0, false",
            "<=2.0.0, 2.0.0, true",
            "~1.2.3, 1.2.9, true",
            "~1.2.3, 1.3.0, false",
            "~1, 1.9.0, true",
            "~1, 2.0.0, false",
            "^1.2.3, 1.9.0, true",
            "^1.2.3, 2.0.0, false",
            "^0.2.3, 0.2.9, true",
            "^0.2.3, 0.3.0, false",
            "^0.0.3, 0.0.4, false",
            ">=1.0.0 <2.0.0, 1.5.0, true",
            ">=1.0.0 <2.0.0, 2.1.0, false",
            "^1.0.0 || ^3.0.0, 3.2.0, true",
            "^1.0.0 || ^3.0.0, 2.2.0, false",
    })
    void matchesVersions(String range, String version, boolean matches) {
        assertThat(VersionRange.parse(range).test(version)).isEqualTo(matches);
    }

    /**
     * Checks that a version that cannot be parsed matches {@code *} and no other range.
     */
    @Test
    void unparsableVersionsOnlyMatchAnyRange() {
        assertThat(VersionRange.parse("*").test("weird-version")).isTrue();
        assertThat(VersionRange.parse(">=1.0.0").test("weird-version")).isFalse();
    }

    /**
     * Checks that {@link VersionRange#parse} throws an {@link IllegalArgumentException} for an invalid range.
     */
    @Test
    void rejectsInvalidRanges() {
        assertThatThrownBy(() -> VersionRange.parse(">=banana")).isInstanceOf(IllegalArgumentException.class);
    }
}
