package de.waterdu.atlantis.trident;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class TineValidatorTest {

    private static Compilation compile(String body) {
        return javac()
                .withProcessors(new TineValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.trident.Tine;

                        """ + body));
    }

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
