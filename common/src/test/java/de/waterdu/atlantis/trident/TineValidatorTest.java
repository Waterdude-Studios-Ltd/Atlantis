package de.waterdu.atlantis.trident;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests that {@link TineValidator} checks the shape and the location of {@link Tine.Barb} methods.
 */
class TineValidatorTest {

    /**
     * Compiles the class {@code test.Sample} with {@link TineValidator}. The source imports {@link Tine}.
     *
     * @param body the source of the class, after the package and import lines
     * @return the result of the compilation
     */
    private static Compilation compile(String body) {
        return javac()
                .withProcessors(new TineValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.trident.Tine;

                        """ + body));
    }

    /**
     * Checks that a static, no-argument, void barb in a {@link Tine} class compiles without warnings.
     */
    @Test
    void acceptsStaticNoArgVoidBarbs() {
        var compilation = compile("""
                @Tine
                public class Sample {
                    @Tine.Barb
                    private static void barb() {}
                }
                """);

        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Checks that a barb that is not static, has arguments or returns a value causes one error on its line.
     *
     * @param method the source of the barb method
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "void barb() {}",
            "static void barb(String argument) {}",
            "static int barb() { return 0; }",
    })
    void rejectsOtherBarbShapes(String method) {
        var compilation = compile("""
                @Tine
                public class Sample {
                    @Tine.Barb
                    %s
                }
                """.formatted(method));

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("barb must be a static ()V method").inFile(compilation.sourceFiles().getFirst()).onLineContaining("barb(");
    }

    /**
     * Checks that a barb in a class without {@link Tine} causes one error.
     */
    @Test
    void rejectsBarbsOutsideTineClasses() {
        var compilation = compile("""
                public class Sample {
                    @Tine.Barb
                    static void barb() {}
                }
                """);

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("barb must be declared in a @Tine class");
    }

    /**
     * Checks that a {@link Tine} class with two barbs causes one error.
     */
    @Test
    void rejectsMultipleBarbs() {
        var compilation = compile("""
                @Tine
                public class Sample {
                    @Tine.Barb
                    static void first() {}

                    @Tine.Barb
                    static void second() {}
                }
                """);

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("tine class has more than one @Tine.Barb method");
    }

    /**
     * Checks that a {@link Tine} class without a barb compiles with a warning.
     */
    @Test
    void warnsAboutTineClassesWithoutBarbs() {
        var compilation = compile("""
                @Tine
                public class Sample {}
                """);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("tine class has no @Tine.Barb method");
    }
}
