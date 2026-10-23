package de.waterdu.atlantis.util.bootstrap;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class BootstrappedValidatorTest {

    private static Compilation compile(String body) {
        return javac()
                .withProcessors(new BootstrappedValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
                        import de.waterdu.atlantis.util.bootstrap.Entrypoint;

                        """ + body));
    }

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
