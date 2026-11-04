package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests {@link UniquelyNamed}.
 */
class UniquelyNamedTest {

    /**
     * Holds a name for the tests of {@link UniquelyNamed#get}.
     *
     * @param name the unique name
     */
    private record Named(String name) implements UniquelyNamed {
        /**
         * @return the name of this record
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Holds a name for the tests of {@link UniquelyNamed#cache()} and {@link UniquelyNamed#isUnique()}.
     *
     * @param name the unique name
     */
    private record Cached(String name) implements UniquelyNamed {
        /**
         * @return the name of this record
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Holds a name that differs from the name it is requested with.
     *
     * @param name the unique name
     */
    private record Mismatched(String name) implements UniquelyNamed {
        /**
         * @return the name of this record
         */
        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Checks that {@link UniquelyNamed#get} returns the same instance for the same name and a different one for
     * another name.
     */
    @Test
    void getCreatesOncePerName() {
        var first = UniquelyNamed.get(Named.class, "a", () -> new Named("a"));
        var second = UniquelyNamed.get(Named.class, "a", () -> new Named("a"));
        var other = UniquelyNamed.get(Named.class, "b", () -> new Named("b"));

        assertThat(second).isSameAs(first);
        assertThat(other).isNotSameAs(first);
    }

    /**
     * Checks that {@link UniquelyNamed#get} throws an {@link IllegalArgumentException} if the created instance has a
     * different name.
     */
    @Test
    void getRejectsAMismatchedName() {
        assertThatIllegalArgumentException().isThrownBy(() -> UniquelyNamed.get(Mismatched.class, "expected", () -> new Mismatched("other")));
    }

    /**
     * Checks that after {@link UniquelyNamed#cache()}, only the cached instance is unique, and checks
     * {@link UniquelyNamed#nameMatches}.
     */
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
