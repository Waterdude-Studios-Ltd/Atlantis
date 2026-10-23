package de.waterdu.atlantis.util.java;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionUtilsTest {

    @SuppressWarnings("unused")
    private static final class Fields {
        String plain;
        Map<String, List<Integer>> parameterized;
        List<String>[] genericArray;
        int primitive;

        public String greet(String name) {
            return "hi " + name;
        }

        public void fail() {
            throw new IllegalStateException("expected");
        }
    }

    @Test
    void typeForField() throws NoSuchFieldException {
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("plain"))).isEqualTo(String.class);
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("primitive"))).isEqualTo(int.class);
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("parameterized")))
                .isEqualTo(new TypeToken<Map<String, List<Integer>>>() {}.getType());
        assertThat(ReflectionUtils.getTypeForField(Fields.class.getDeclaredField("genericArray")))
                .isEqualTo(new TypeToken<List<String>[]>() {}.getType());
    }

    @Test
    void doesClassExist() {
        assertThat(ReflectionUtils.doesClassExist("java.lang.String")).isTrue();
        assertThat(ReflectionUtils.doesClassExist("de.waterdu.atlantis.DoesNotExist")).isFalse();
    }

    @Test
    void invokeSafely() throws NoSuchMethodException {
        var fields = new Fields();

        String greeting = ReflectionUtils.invokeSafely(Fields.class.getMethod("greet", String.class), fields, "water");
        Object failed = ReflectionUtils.invokeSafely(Fields.class.getMethod("fail"), fields);

        assertThat(greeting).isEqualTo("hi water");
        assertThat(failed).isNull();
    }
}
