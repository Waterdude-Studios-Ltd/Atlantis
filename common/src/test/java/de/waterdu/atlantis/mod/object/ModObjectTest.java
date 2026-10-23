package de.waterdu.atlantis.mod.object;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ModObjectTest {

    private static final ModObjectKey KEY = new ModObjectKey("atlantis", "thing");

    @Test
    void keyExposesNamespaceAndObject() {
        assertThat(KEY.namespace()).isEqualTo("atlantis");
        assertThat(KEY.object()).isEqualTo("thing");
        assertThat(KEY.type()).isSameAs(KEY);
        assertThat(KEY).isEqualTo(new ModObjectKey("atlantis", "thing"));
    }

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

    @Test
    void objectDereferencesItsReference() {
        var object = new ModObject(KEY, "value");

        String value = object.dereference();
        assertThat(value).isEqualTo("value");
        assertThat(object.dereference(String.class)).isEqualTo("value");
        assertThat(object.typeMatches(KEY)).isTrue();
    }

    @Test
    void futureDereferencesItsFuture() {
        var future = CompletableFuture.completedFuture(5);
        var modFuture = new ModFuture(KEY, future);

        assertThat(modFuture.<Integer>dereference()).isSameAs(future);
        assertThat(modFuture.dereference(Integer.class)).isSameAs(future);
    }
}
