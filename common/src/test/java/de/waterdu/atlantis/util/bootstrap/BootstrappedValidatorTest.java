package de.waterdu.atlantis.util.bootstrap;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests that {@link BootstrappedValidator} checks the shape and the location of {@link Entrypoint} methods.
 */
class BootstrappedValidatorTest {

    /**
     * Compiles the class {@code test.Sample} with {@link BootstrappedValidator}. The source imports
     * {@link Bootstrapped} and {@link Entrypoint}.
     *
     * @param body the source of the class, after the package and import lines
     * @return the result of the compilation
     */
    private static Compilation compile(String body) {
        return javac()
                .withProcessors(new BootstrappedValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
                        import de.waterdu.atlantis.util.bootstrap.Entrypoint;

                        """ + body));
    }

    /**
     * Checks that a static, no-argument, void entrypoint in a {@link Bootstrapped} class compiles without warnings.
     */
    @Test
    void acceptsStaticNoArgVoidEntrypoints() {
        var compilation = compile("""
                @Bootstrapped(order = 5)
                public class Sample {
                    @Entrypoint
                    private static void bootstrap() {}
                }
                """);

        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Checks that an entrypoint that is not static, has arguments or returns a value fails the compilation with one
     * error on its line.
     *
     * @param method the source of the entrypoint method
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "void bootstrap() {}",
            "static void bootstrap(String argument) {}",
            "static int bootstrap() { return 0; }",
    })
    void rejectsOtherEntrypointShapes(String method) {
        var compilation = compile("""
                @Bootstrapped
                public class Sample {
                    @Entrypoint
                    %s
                }
                """.formatted(method));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("entrypoint must be a static ()V method").inFile(compilation.sourceFiles().getFirst()).onLineContaining("bootstrap(");
    }

    /**
     * Checks that an entrypoint in a class without {@link Bootstrapped} causes one error.
     */
    @Test
    void rejectsEntrypointsOutsideBootstrappedClasses() {
        var compilation = compile("""
                public class Sample {
                    @Entrypoint
                    static void bootstrap() {}
                }
                """);

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("entrypoint must be declared in a bootstrapped class");
    }

    /**
     * Checks that a {@link Bootstrapped} class without an entrypoint compiles with a warning.
     */
    @Test
    void warnsAboutBootstrappedClassesWithoutEntrypoints() {
        var compilation = compile("""
                @Bootstrapped
                public class Sample {
                    static void notAnEntrypoint() {}
                }
                """);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("bootstrapped class has no @Entrypoint method");
    }
}
