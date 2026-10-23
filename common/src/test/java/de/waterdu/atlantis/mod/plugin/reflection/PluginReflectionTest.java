package de.waterdu.atlantis.mod.plugin.reflection;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PluginReflectionTest {

    private static String unique(String name) {
        return name + "-" + UUID.randomUUID();
    }

    @Test
    void classIsCachedByName() {
        var name = unique("Target");
        var lookups = new AtomicInteger();

        var first = PluginClass.of(name, () -> {
            lookups.incrementAndGet();
            return Target.class;
        });
        var second = PluginClass.<Target>of(name, () -> {
            throw new AssertionError("should be cached");
        });

        assertThat(second).isSameAs(first);
        assertThat(first.clazz()).isEqualTo(Target.class);
        assertThat(lookups).hasValue(1);
    }

    @Test
    void readsPrivateStaticAndInstanceFields() {
        var target = PluginClass.of(unique("Target"), () -> Target.class);
        PluginField<String> staticField = target.field("STATIC", () -> field("STATIC"));
        PluginField<Integer> instanceField = target.field("value", () -> field("value"));

        assertThat(staticField.getStatic()).isEqualTo("static");
        assertThat(instanceField.get(new Target(7))).isEqualTo(7);
        assertThat(target.<String>field("STATIC", () -> {
            throw new AssertionError("should be cached");
        })).isSameAs(staticField);
    }

    @Test
    void invokesPrivateStaticAndInstanceMethods() {
        var target = PluginClass.of(unique("Target"), () -> Target.class);
        PluginMethod<String> greet = target.method("greet", () -> method("greet", String.class));
        PluginMethod<Integer> doubled = target.method("doubled", () -> method("doubled"));

        assertThat(greet.invokeStatic("world")).isEqualTo("hello world");
        assertThat(doubled.invoke(new Target(4))).isEqualTo(8);
    }

    @Test
    void failuresReturnNull() {
        PluginField<Integer> instanceField = PluginField.of(unique("Target#value"), () -> field("value"));
        PluginMethod<Integer> doubled = PluginMethod.of(unique("Target#doubled"), () -> method("doubled"));
        PluginMethod<String> greet = PluginMethod.of(unique("Target#greet"), () -> method("greet", String.class));

        assertThat(instanceField.getStatic()).isNull();
        assertThat(doubled.invokeStatic()).isNull();
        assertThat(greet.invokeStatic(1)).isNull();
    }

    @Test
    void objectWrapsInstanceAndClass() {
        var instance = new Target(1);
        var object = new PluginObject<>(instance);

        assertThat(object.instance()).isSameAs(instance);
        assertThat(object.clazz().clazz()).isEqualTo(Target.class);
        assertThat(new PluginObject<Target>(null).clazz()).isNull();

        var explicit = PluginClass.of(unique("Target"), () -> Target.class);
        assertThat(new PluginObject<>(explicit, instance).clazz()).isSameAs(explicit);
    }

    private static java.lang.reflect.Field field(String name) {
        try {
            return Target.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static java.lang.reflect.Method method(String name, Class<?>... parameters) {
        try {
            return Target.class.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unused")
    private record Target(int value) {

        private static final String STATIC = "static";

        private static String greet(String name) {
            return "hello " + name;
        }

        private int doubled() {
            return this.value * 2;
        }
    }
}
