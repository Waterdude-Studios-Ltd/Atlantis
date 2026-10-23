package de.waterdu.atlantis.util.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KeysTest {

    private static final Key<String> A = Key.of("atlantis", "a");
    private static final Key<String> B = Key.of("atlantis", "b");
    private static final Key<String> C = Key.of("atlantis", "c");

    @Test
    void emptyAndOf() {
        assertThat(Keys.<String>empty()).isEmpty();
        assertThat(Keys.of(A, B, A)).hasSize(2).containsExactlyInAnyOrder(A, B);
    }

    @Test
    void behavesAsASet() {
        var keys = Keys.<String>empty();

        assertThat(keys.add(A)).isTrue();
        assertThat(keys.add(A)).isFalse();
        assertThat(keys.addAll(List.of(B, C))).isTrue();
        assertThat(keys.contains(Key.of("atlantis", "b"))).isTrue();
        assertThat(keys.containsAll(List.of(A, C))).isTrue();
        assertThat(keys.remove(B)).isTrue();
        assertThat(keys.retainAll(List.of(A))).isTrue();
        assertThat(keys).containsExactly(A);
        assertThat(keys.toArray()).containsExactly(A);
        assertThat(keys.toArray(new Key<?>[0])).containsExactly(A);
        assertThat(keys.removeAll(List.of(A))).isTrue();
        assertThat(keys.isEmpty()).isTrue();

        keys.add(C);
        keys.clear();
        assertThat(keys.size()).isZero();
    }

    @Test
    void equalsFollowsTheSetContract() {
        var keys = Keys.of(A, B);

        assertThat(keys).isEqualTo(Set.of(A, B)).hasSameHashCodeAs(Set.of(A, B));
        assertThat(Set.of(A, B)).isEqualTo(keys);
        assertThat(keys).isEqualTo(Keys.of(B, A)).isNotEqualTo(Keys.of(A));
    }
}
