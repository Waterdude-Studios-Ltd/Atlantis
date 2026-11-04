package de.waterdu.atlantis.mod.object;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ModObjectKey}, {@link ModObject} and {@link ModFuture}.
 */
class ModObjectTest {

    private static final ModObjectKey KEY = new ModObjectKey("atlantis", "thing");

    /**
     * Checks that a key gives its namespace and object, is its own type, and equals a key with the same values.
     */
    @Test
    void keyExposesNamespaceAndObject() {
        assertThat(KEY.namespace()).isEqualTo("atlantis");
        assertThat(KEY.object()).isEqualTo("thing");
        assertThat(KEY.type()).isSameAs(KEY);
        assertThat(KEY).isEqualTo(new ModObjectKey("atlantis", "thing"));
    }

    /**
     * Checks that the type matches only when the namespace and the object both match.
     */
    @Test
    void typeMatching() {
        var same = new ModObjectKey("atlantis", "thing");
        var otherNamespace = new ModObjectKey("pixelmon", "thing");
        var otherObject = new ModObjectKey("atlantis", "other");

        assertThat(KEY.typeMatches(same)).isTrue();
        assertThat(KEY.namespaceMatches(otherNamespace)).isFalse();
        assertThat(KEY.objectMatches(otherNamespace)).isTrue();
        assertThat(KEY.typeMatches(otherNamespace)).isFalse();
        assertThat(KEY.namespaceMatches(otherObject)).isTrue();
        assertThat(KEY.typeMatches(otherObject)).isFalse();
    }

    /**
     * Checks that a {@link ModObject} dereferences to the value it holds and matches its key.
     */
    @Test
    void objectDereferencesItsReference() {
        var object = new ModObject(KEY, "value");

        String value = object.dereference();
        assertThat(value).isEqualTo("value");
        assertThat(object.dereference(String.class)).isEqualTo("value");
        assertThat(object.typeMatches(KEY)).isTrue();
    }

    /**
     * Checks that a {@link ModFuture} dereferences to the future it holds, not to the value of the future.
     */
    @Test
    void futureDereferencesItsFuture() {
        var future = CompletableFuture.completedFuture(5);
        var modFuture = new ModFuture(KEY, future);

        assertThat(modFuture.<Integer>dereference()).isSameAs(future);
        assertThat(modFuture.dereference(Integer.class)).isSameAs(future);
    }
}
