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

@ExtendWith(AtlantisTestEnvironment.class)
class BootstrapperTest {

    static final List<String> CALLS = new ArrayList<>();

    @BeforeEach
    void clearCalls() {
        CALLS.clear();
    }

    @Test
    void installsTheTestLoader(TestModLoader loader) {
        assertThat(Mods.<TestModLoader>modLoader()).isSameAs(loader);
    }

    @Test
    void invokesEntrypointsInOrder(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Late.class, Early.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("early", "default", "late.first", "late.second");
    }

    @Test
    void bootstrapsOnlyOnce(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Default.class);

        Bootstrapper.bootstrap(loader);
        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("default");
    }

    @Test
    void skipsClassesThatCannotBeLoaded(TestModLoader loader) {
        loader.annotatedName(Bootstrapped.class, "de.waterdu.atlantis.DoesNotExist");
        loader.annotated(Bootstrapped.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("default");
    }

    @Test
    void failingEntrypointDoesNotStopOthers(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Failing.class, Default.class);

        Bootstrapper.bootstrap(loader);

        assertThat(CALLS).containsExactly("failing", "default");
    }

    @Test
    void forEachAnnotatedClassReportsTheAnnotation(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Early.class);
        Bootstrapper.bootstrap(loader);
        var seen = new ArrayList<Integer>();

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, annotated -> seen.add(((Bootstrapped) annotated.annotation()).order()));

        assertThat(seen).containsExactly(-10);
    }

    @Test
    void forEachAnnotatedClassWithNoMatchesDoesNothing(TestModLoader loader) {
        Bootstrapper.bootstrap(loader);

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, __ -> CALLS.add("unexpected"));

        assertThat(CALLS).isEmpty();
    }

    @Bootstrapped(order = 10)
    static final class Late {

        @Entrypoint
        static void first() {
            CALLS.add("late.first");
        }

        @Entrypoint
        static void second() {
            CALLS.add("late.second");
        }
    }

    @Bootstrapped(order = -10)
    static final class Early {

        @Entrypoint
        static void run() {
            CALLS.add("early");
        }
    }

    @Bootstrapped
    static final class Default {

        @Entrypoint
        static void run() {
            CALLS.add("default");
        }
    }

    @Bootstrapped(order = -20)
    static final class Failing {

        @Entrypoint
        static void run() {
            CALLS.add("failing");
            throw new IllegalStateException("expected by the test");
        }
    }
}
