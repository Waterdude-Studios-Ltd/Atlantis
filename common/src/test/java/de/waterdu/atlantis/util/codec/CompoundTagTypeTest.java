package de.waterdu.atlantis.util.codec;

import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class CompoundTagTypeTest {

    static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(int.class, CompoundTagType.INT_PRIMITIVE, 5),
                Arguments.of(long.class, CompoundTagType.LONG_PRIMITIVE, 5L),
                Arguments.of(boolean.class, CompoundTagType.BOOLEAN_PRIMITIVE, true),
                Arguments.of(byte.class, CompoundTagType.BYTE_PRIMITIVE, (byte) 5),
                Arguments.of(double.class, CompoundTagType.DOUBLE_PRIMITIVE, 5.5),
                Arguments.of(float.class, CompoundTagType.FLOAT_PRIMITIVE, 5.5f),
                Arguments.of(short.class, CompoundTagType.SHORT_PRIMITIVE, (short) 5),
                Arguments.of(Integer.class, CompoundTagType.INTEGER_OBJECT, 5),
                Arguments.of(Long.class, CompoundTagType.LONG_OBJECT, 5L),
                Arguments.of(Boolean.class, CompoundTagType.BOOLEAN_OBJECT, true),
                Arguments.of(Byte.class, CompoundTagType.BYTE_OBJECT, (byte) 5),
                Arguments.of(Double.class, CompoundTagType.DOUBLE_OBJECT, 5.5),
                Arguments.of(Float.class, CompoundTagType.FLOAT_OBJECT, 5.5f),
                Arguments.of(Short.class, CompoundTagType.SHORT_OBJECT, (short) 5),
                Arguments.of(String.class, CompoundTagType.STRING, "value"),
                Arguments.of(UUID.class, CompoundTagType.UUID, new UUID(1, 2))
        );
    }

    @ParameterizedTest
    @MethodSource("values")
    void scalarTypesRoundTrip(Type type, CompoundTagType expected, Object value) {
        var tagType = CompoundTagType.get(type).orElseThrow();
        var tag = new CompoundTag();

        tagType.write(tag, "key", value);

        assertThat(tagType).isEqualTo(expected);
        assertThat((Object) tagType.read(tag, "key")).isEqualTo(value);
    }

    @Test
    void arraysRoundTrip() {
        var tag = new CompoundTag();
        CompoundTagType.get(int[].class).orElseThrow().write(tag, "ints", new int[]{1, 2});
        CompoundTagType.get(long[].class).orElseThrow().write(tag, "longs", new long[]{3L});
        CompoundTagType.get(byte[].class).orElseThrow().write(tag, "bytes", new byte[]{4});

        assertThat(CompoundTagType.INT_ARRAY.<int[]>read(tag, "ints")).containsExactly(1, 2);
        assertThat(CompoundTagType.LONG_ARRAY.<long[]>read(tag, "longs")).containsExactly(3L);
        assertThat(CompoundTagType.BYTE_ARRAY.<byte[]>read(tag, "bytes")).containsExactly((byte) 4);
    }

    @Test
    void listsAreStoredAsArrays() {
        var tag = new CompoundTag();
        CompoundTagType.get(TypeToken.getParameterized(List.class, Integer.class).getType()).orElseThrow().write(tag, "ints", List.of(1, 2));
        CompoundTagType.get(TypeToken.getParameterized(List.class, Long.class).getType()).orElseThrow().write(tag, "longs", List.of(3L));
        CompoundTagType.get(TypeToken.getParameterized(List.class, Byte.class).getType()).orElseThrow().write(tag, "bytes", List.of((byte) 4));

        assertThat(CompoundTagType.INTEGER_LIST.<int[]>read(tag, "ints")).containsExactly(1, 2);
        assertThat(CompoundTagType.LONG_LIST.<long[]>read(tag, "longs")).containsExactly(3L);
        assertThat(CompoundTagType.BYTE_LIST.<byte[]>read(tag, "bytes")).containsExactly((byte) 4);
    }

    @Test
    void unknownTypeIsEmpty() {
        assertThat(CompoundTagType.get(Object.class)).isEmpty();
        assertThat(CompoundTagType.get("not.a.Type")).isEmpty();
    }
}
