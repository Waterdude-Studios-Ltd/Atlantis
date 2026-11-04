package de.waterdu.atlantis.util.bootstrap;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.TestModLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link Bootstrapper} finds and runs the entrypoints of {@link Bootstrapped} classes.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class BootstrapperTest {

    static final List<String> CALLS = new ArrayList<>();

    /**
     * Clears {@link #CALLS} before each test.
     */
    @BeforeEach
    void clearCalls() {
        CALLS.clear();
    }

    /**
     * Checks that {@link Mods#modLoader()} gets the test loader.
     *
     * @param loader the test loader
     */
    @Test
    void installsTheTestLoader(TestModLoader loader) {
        assertThat(Mods.<TestModLoader>modLoader()).isSameAs(loader);
    }

    /**
     * Checks that entrypoints run in order of {@link Bootstrapped#value()}, whatever order the scan reports.
     *
     * @param loader the test loader
     */
    @Test
    void invokesEntrypointsInOrder(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Late.class, Early.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("early", "default", "late.first", "late.second");
    }

    /**
     * Checks that a second call to {@link Bootstrapper#bootstrap} does not run the entrypoints again.
     *
     * @param loader the test loader
     */
    @Test
    void bootstrapsOnlyOnce(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Default.class);

        Bootstrapper.bootstrap(loader);
        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("default");
    }

    /**
     * Checks that a class name that does not exist does not stop the other entrypoints.
     *
     * @param loader the test loader
     */
    @Test
    void skipsClassesThatCannotBeLoaded(TestModLoader loader) {
        loader.annotatedName(Bootstrapped.class, "de.waterdu.atlantis.DoesNotExist");
        loader.annotated(Bootstrapped.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("default");
    }

    /**
     * Checks that an entrypoint that throws does not stop the entrypoints after it.
     *
     * @param loader the test loader
     */
    @Test
    void failingEntrypointDoesNotStopOthers(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Failing.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("failing", "default");
    }

    /**
     * Checks that {@link Bootstrapper#forEachAnnotatedClass} gives each scanned class with its annotation.
     *
     * @param loader the test loader
     */
    @Test
    void forEachAnnotatedClassReportsTheAnnotation(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Early.class);
        Bootstrapper.bootstrap(loader);
        var seen = new ArrayList<Bootstrapped.Priority>();

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, annotated -> seen.add(((Bootstrapped) annotated.annotation()).value()));

        assertThat(seen).containsExactly(Bootstrapped.Priority.HIGH);
    }

    /**
     * Checks that {@link Bootstrapper#forEachAnnotatedClass} does not call the consumer when no class matches.
     *
     * @param loader the test loader
     */
    @Test
    void forEachAnnotatedClassWithNoMatchesDoesNothing(TestModLoader loader) {
        Bootstrapper.bootstrap(loader);

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, __ -> CALLS.add("unexpected"));

        assertThat(CALLS).isEmpty();
    }

    /**
     * Has two entrypoints and bootstraps at {@link Bootstrapped.Priority#LOW}.
     */
    @Bootstrapped(Bootstrapped.Priority.LOW)
    static final class Late {

        /**
         * Adds {@code late.first} to {@link #CALLS}.
         */
        @Entrypoint
        static void first() {
            CALLS.add("late.first");
        }

        /**
         * Adds {@code late.second} to {@link #CALLS}.
         */
        @Entrypoint
        static void second() {
            CALLS.add("late.second");
        }
    }

    /**
     * Bootstraps at {@link Bootstrapped.Priority#HIGH}.
     */
    @Bootstrapped(Bootstrapped.Priority.HIGH)
    static final class Early {

        /**
         * Adds {@code early} to {@link #CALLS}.
         */
        @Entrypoint
        static void run() {
            CALLS.add("early");
        }
    }

    /**
     * Bootstraps at the default priority.
     */
    @Bootstrapped
    static final class Default {

        /**
         * Adds {@code default} to {@link #CALLS}.
         */
        @Entrypoint
        static void run() {
            CALLS.add("default");
        }
    }

    /**
     * Bootstraps at {@link Bootstrapped.Priority#VERY_HIGH} and has an entrypoint that throws.
     */
    @Bootstrapped(Bootstrapped.Priority.VERY_HIGH)
    static final class Failing {

        /**
         * Adds {@code failing} to {@link #CALLS}, then throws.
         *
         * @throws IllegalStateException always
         */
        @Entrypoint
        static void run() {
            CALLS.add("failing");
            throw new IllegalStateException("expected by the test");
        }
    }
}
