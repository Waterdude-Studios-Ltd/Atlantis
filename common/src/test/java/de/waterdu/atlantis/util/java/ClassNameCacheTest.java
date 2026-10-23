package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClassNameCacheTest {

    @Test
    void returnsTheClassNames() {
        assertThat(ClassNameCache.name(Map.Entry.class)).isEqualTo("java.util.Map$Entry");
        assertThat(ClassNameCache.simpleName(Map.Entry.class)).isEqualTo("Entry");
        assertThat(ClassNameCache.canonicalName(Map.Entry.class)).isEqualTo("java.util.Map.Entry");
        assertThat(ClassNameCache.packageName(Map.Entry.class)).isEqualTo("java.util");
        assertThat(ClassNameCache.typeName(String[].class)).isEqualTo("java.lang.String[]");
    }

    @Test
    void anonymousClassHasNoCanonicalName() {
        var anonymous = new Object() {}.getClass();

        assertThat(ClassNameCache.canonicalName(anonymous)).isNull();
        assertThat(ClassNameCache.simpleName(anonymous)).isEmpty();
    }
}
