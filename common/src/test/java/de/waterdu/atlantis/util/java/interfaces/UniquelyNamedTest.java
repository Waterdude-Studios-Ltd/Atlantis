package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UniquelyNamedTest {

    private record Named(String name) implements UniquelyNamed {
        @Override
        public String toString() {
            return this.name;
        }
    }

    private record Cached(String name) implements UniquelyNamed {
        @Override
        public String toString() {
            return this.name;
        }
    }

    private record Mismatched(String name) implements UniquelyNamed {
        @Override
        public String toString() {
            return this.name;
        }
    }

    @Test
    void getCreatesOncePerName() {
        var first = UniquelyNamed.get(Named.class, "a", () -> new Named("a"));
        var second = UniquelyNamed.get(Named.class, "a", () -> new Named("a"));
        var other = UniquelyNamed.get(Named.class, "b", () -> new Named("b"));

        assertThat(second).isSameAs(first);
        assertThat(other).isNotSameAs(first);
    }

    @Test
    void getRejectsAMismatchedName() {
        assertThatIllegalArgumentException().isThrownBy(() -> UniquelyNamed.get(Mismatched.class, "expected", () -> new Mismatched("other")));
    }

    @Test
    void cacheAndIsUnique() {
        var original = new Cached("x");
        var duplicate = new Cached("x");

        assertThat(duplicate.isUnique()).isTrue();
        original.cache();

        assertThat(original.isUnique()).isTrue();
        assertThat(duplicate.isUnique()).isFalse();
        assertThat(original.nameMatches("x")).isTrue();
        assertThat(original.nameMatches("y")).isFalse();
    }
}
