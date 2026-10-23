package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UUIDUtilsTest {

    private static final UUID UUID_VALUE = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void parsesDashedAndUndashed() {
        assertThat(UUIDUtils.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")).isEqualTo(UUID_VALUE);
        assertThat(UUIDUtils.fromString("069a79f444e94726a5befca90e38aaf5")).isEqualTo(UUID_VALUE);
    }

    @Test
    void rejectsInvalid() {
        assertThatIllegalArgumentException().isThrownBy(() -> UUIDUtils.fromString("not-a-uuid"));
    }

    @Test
    void zeroed() {
        assertThat(UUIDUtils.ZEROED_UUID).isEqualTo(new UUID(0, 0));
    }

    @Test
    void seededUuidsAreDeterministicVersion4() {
        var a = UUIDUtils.fromSeed(42);

        assertThat(UUIDUtils.fromSeed(42)).isEqualTo(a);
        assertThat(UUIDUtils.fromSeed(43)).isNotEqualTo(a);
        assertThat(a.version()).isEqualTo(4);
        assertThat(a.variant()).isEqualTo(2);
    }

    @Test
    void fromHashCodeUsesTheHash() {
        assertThat(UUIDUtils.fromHashCode("water")).isEqualTo(UUIDUtils.fromSeed("water".hashCode()));
    }
}
