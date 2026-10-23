package de.waterdu.atlantis.file.auto;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class AtlantisConfigValidatorTest {

    private static final String DATA_OVERRIDES = """
                @Override public int hashCode() { return 0; }
                @Override public boolean equals(Object other) { return false; }
                @Override public String toString() { return ""; }
            """;

    private static Compilation compile(String body) {
        return javac()
                .withProcessors(new AtlantisConfigValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.file.auto.*;
                        import de.waterdu.atlantis.file.datatypes.*;

                        """ + body));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@BasicConfiguration(mod = \"m\")",
            "@I18nFile(mod = \"m\")",
            "@FileBackedObject(mod = \"m\", path = \"sample.json\")",
    })
    void acceptsConfigurations(String annotation) {
        assertThat(compile(annotation + "\npublic class Sample implements Configuration {}")).succeededWithoutWarnings();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@BasicConfiguration(mod = \"m\")",
            "@BasicData(mod = \"m\")",
            "@I18nFile(mod = \"m\")",
            "@FileBackedObject(mod = \"m\", path = \"sample.json\")",
    })
    void rejectsClassesThatAreNotConfigurations(String annotation) {
        var compilation = compile(annotation + "\npublic class Sample {}");

        var kind = annotation.substring(1, annotation.indexOf('('));
        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("invalid " + kind + " implementation, must implement one of Configuration, Data, NamedData, PlayerData");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Data", "NamedData", "PlayerData"})
    void acceptsDataThatOverridesObjectMethods(String type) {
        var compilation = compile("@BasicData(mod = \"m\")\npublic abstract class Sample implements " + type + " {\n" + DATA_OVERRIDES + "}");

        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void acceptsRecordsImplementingData() {
        var compilation = compile("""
                @BasicData(mod = "m")
                public record Sample(java.util.UUID uuid, String identifier) implements Data {
                    public void setUUID(java.util.UUID uuid) {}
                }
                """);

        assertThat(compilation).succeededWithoutWarnings();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Data", "PlayerData"})
    void reportsMissingOverridesOnce(String type) {
        var compilation = compile("""
                @BasicData(mod = "m")
                public abstract class Sample implements %s {
                    @Override public int hashCode() { return 0; }
                }
                """.formatted(type));

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("invalid Sample implementation, must override equals, toString");
    }

    @Test
    void reportsEveryMissingOverride() {
        var compilation = compile("@BasicData(mod = \"m\")\npublic abstract class Sample implements Data {}");

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("must override hashCode, equals, toString");
    }
}
