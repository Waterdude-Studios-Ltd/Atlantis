package de.waterdu.atlantis.mod.plugin.reflection;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link PluginClass}, {@link PluginField}, {@link PluginMethod} and {@link PluginObject}.
 */
class PluginReflectionTest {

    /**
     * Adds a random UUID to a name, so that each test has its own cache entry.
     *
     * @param name the name
     * @return the name, a {@code -} and a random UUID
     */
    private static String unique(String name) {
        return name + "-" + UUID.randomUUID();
    }

    /**
     * Checks that {@link PluginClass#of} looks up a class one time and then gives the cached instance for its name.
     */
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

    /**
     * Checks that plugin fields read private static and instance fields, and that a field is cached by name.
     */
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

    /**
     * Checks that plugin methods invoke private static and instance methods.
     */
    @Test
    void invokesPrivateStaticAndInstanceMethods() {
        var target = PluginClass.of(unique("Target"), () -> Target.class);
        PluginMethod<String> greet = target.method("greet", () -> method("greet", String.class));
        PluginMethod<Integer> doubled = target.method("doubled", () -> method("doubled"));

        assertThat(greet.invokeStatic("world")).isEqualTo("hello world");
        assertThat(doubled.invoke(new Target(4))).isEqualTo(8);
    }

    /**
     * Checks that static access to instance members, and calls with incorrect arguments, return {@code null}.
     */
    @Test
    void failuresReturnNull() {
        PluginField<Integer> instanceField = PluginField.of(unique("Target#value"), () -> field("value"));
        PluginMethod<Integer> doubled = PluginMethod.of(unique("Target#doubled"), () -> method("doubled"));
        PluginMethod<String> greet = PluginMethod.of(unique("Target#greet"), () -> method("greet", String.class));

        assertThat(instanceField.getStatic()).isNull();
        assertThat(doubled.invokeStatic()).isNull();
        assertThat(greet.invokeStatic(1)).isNull();
    }

    /**
     * Checks that a {@link PluginObject} keeps its instance and gets its class from the instance or from the
     * constructor.
     */
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

    /**
     * Gets a declared field of {@link Target}.
     *
     * @param name the name of the field
     * @return the field
     * @throws AssertionError if the field does not exist
     */
    private static java.lang.reflect.Field field(String name) {
        try {
            return Target.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Gets a declared method of {@link Target}.
     *
     * @param name the name of the method
     * @param parameters the parameter types of the method
     * @return the method
     * @throws AssertionError if the method does not exist
     */
    private static java.lang.reflect.Method method(String name, Class<?>... parameters) {
        try {
            return Target.class.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Has the private members that the tests reach through reflection.
     *
     * @param value the value of the instance
     */
    @SuppressWarnings("unused")
    private record Target(int value) {

        private static final String STATIC = "static";

        /**
         * Greets a name.
         *
         * @param name the name to greet
         * @return {@code hello} and the name, separated by a space
         */
        private static String greet(String name) {
            return "hello " + name;
        }

        /**
         * @return two times the value
         */
        private int doubled() {
            return this.value * 2;
        }
    }
}
