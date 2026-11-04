package de.waterdu.atlantis.util.java;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ReflectionUtils}.
 */
class ReflectionUtilsTest {

    /**
     * Holds fields and methods for the reflection tests to look up.
     */
    @SuppressWarnings("unused")
    private static final class Fields {
        String plain;
        Map<String, List<Integer>> parameterized;
        List<String>[] genericArray;
        int primitive;

        /**
         * Creates a greeting for a name.
         *
         * @param name the name to greet
         * @return {@code "hi "} followed by the name
         */
        public String greet(String name) {
            return "hi " + name;
        }

        /**
         * Always throws an {@link IllegalStateException}.
         *
         * @throws IllegalStateException always
         */
        public void fail() {
            throw new IllegalStateException("expected");
        }
    }

    /**
     * Checks that {@link ReflectionUtils#getTypeForField} returns the full generic type of plain, primitive,
     * parameterized and generic array fields.
     */
    @Test
    void typeForField() throws NoSuchFieldException {
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("plain"))).isEqualTo(String.class);
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("primitive"))).isEqualTo(int.class);
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("parameterized")))
                .isEqualTo(new TypeToken<Map<String, List<Integer>>>() {}.getType());
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("genericArray")))
                .isEqualTo(new TypeToken<List<String>[]>() {}.getType());
    }

    /**
     * Checks that {@link ReflectionUtils#doesClassExist} finds present classes and not absent ones.
     */
    @Test
    void doesClassExist() {
        assertThat(ReflectionUtils.doesClassExist("java.lang.String")).isTrue();
        assertThat(ReflectionUtils.doesClassExist("de.waterdu.atlantis.DoesNotExist")).isFalse();
    }

    /**
     * Checks that {@link ReflectionUtils#invokeSafely} returns the result of the method, or {@code null} if the
     * method throws.
     */
    @Test
    void invokeSafely() throws NoSuchMethodException {
        var fields = new Fields();

        String greeting = ReflectionUtils.invokeSafely(Fields.class.getMethod("greet", String.class), fields, "water");
        Object failed = ReflectionUtils.invokeSafely(Fields.class.getMethod("fail"), fields);

        assertThat(greeting).isEqualTo("hi water");
        assertThat(failed).isNull();
    }
}
