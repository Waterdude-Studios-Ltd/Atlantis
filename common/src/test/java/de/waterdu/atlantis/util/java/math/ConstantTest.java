package de.waterdu.atlantis.util.java.math;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Constant}.
 */
class ConstantTest {

    /**
     * Checks that {@link Constant#get} finds a constant by any of its names or symbols, and returns {@code null}
     * for an unknown name.
     */
    @Test
    void lookupByAnyRepresentation() {
        assertThat(Constant.get("pi")).isEqualTo(Constant.PI);
        assertThat(Constant.get("PI")).isEqualTo(Constant.PI);
        assertThat(Constant.get("π")).isEqualTo(Constant.PI);
        assertThat(Constant.get("gravity")).isEqualTo(Constant.G);
        assertThat(Constant.get("nothing")).isNull();
    }

    /**
     * Checks the values of some constants and the representations of {@link Constant#TAU}.
     */
    @Test
    void values() {
        assertThat(Constant.PI.value()).isEqualTo(Math.PI);
        assertThat(Constant.TAU.value()).isEqualTo(2 * Math.PI);
        assertThat(Constant.E.value()).isEqualTo(Math.E);
        assertThat(Constant.SQRT2.value() * Constant.SQRT2.value()).isCloseTo(2.0, org.assertj.core.api.Assertions.within(1e-12));
        assertThat(Constant.TAU.representations()).containsExactly("tau", "τ");
    }
}
