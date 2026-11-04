package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class names that {@link ClassNameCache} returns.
 */
class ClassNameCacheTest {

    /**
     * Checks that the name, simple name, canonical name, package name and type name match those of the class.
     */
    @Test
    void returnsTheClassNames() {
        assertThat(ClassNameCache.name(Map.Entry.class)).isEqualTo("java.util.Map$Entry");
        assertThat(ClassNameCache.simpleName(Map.Entry.class)).isEqualTo("Entry");
        assertThat(ClassNameCache.canonicalName(Map.Entry.class)).isEqualTo("java.util.Map.Entry");
        assertThat(ClassNameCache.packageName(Map.Entry.class)).isEqualTo("java.util");
        assertThat(ClassNameCache.typeName(String[].class)).isEqualTo("java.lang.String[]");
    }

    /**
     * Checks that an anonymous class has a {@code null} canonical name and an empty simple name.
     */
    @Test
    void anonymousClassHasNoCanonicalName() {
        var anonymous = new Object() {}.getClass();

        assertThat(ClassNameCache.canonicalName(anonymous)).isNull();
        assertThat(ClassNameCache.simpleName(anonymous)).isEmpty();
    }
}
