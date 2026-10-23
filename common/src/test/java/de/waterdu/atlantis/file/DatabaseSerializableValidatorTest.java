package de.waterdu.atlantis.file;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Compiles against the already compiled DatabaseEncoders, the way a mod depending on Atlantis would.
 */
class DatabaseSerializableValidatorTest {

    private static Compilation compile(String field) {
        return javac()
                .withProcessors(new DatabaseSerializableValidator())
                .compile(JavaFileObjects.forSourceString("test.Sample", """
                        package test;

                        import de.waterdu.atlantis.file.DatabaseEncoders;
                        import de.waterdu.atlantis.file.DatabaseSerializable;

                        public class Sample {
                            %s
                        }
                        """.formatted(field)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) int value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) Integer value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.BOOLEAN) boolean value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.UUID) java.util.UUID value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.TEXT) String value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.VARCHAR_UUID) java.util.UUID value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.VARCHAR_UUID) String value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.BIGINT) java.math.BigInteger value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.JSON) java.util.List<String> value;",
            "@DatabaseSerializable java.util.Map<String, Integer> value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.LONGTEXT) Object value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.VARCHAR) Object value;",
            "@DatabaseSerializable(type = \"custom_type\") Object value;",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.STRING) @org.jspecify.annotations.Nullable String value;",
    })
    void acceptsAllowedTypes(String field) {
        assertThat(compile(field)).succeededWithoutWarnings();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) String value; | int | java.lang.String",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) long value; | int | long",
            "@DatabaseSerializable(type = \"INT\") String value; | INT | java.lang.String",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.VARCHAR_UUID) int value; | varchar(36) | int",
            "@DatabaseSerializable(type = DatabaseEncoders.Defaults.UUID) String value; | uuid | java.lang.String",
            "@Deprecated @DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) String value; | int | java.lang.String",
    })
    void rejectsOtherTypes(String field, String sqlType, String javaType) {
        var compilation = compile(field);

        assertThat(compilation).hadErrorCount(1);
        assertThat(compilation).hadErrorContaining("Unsupported SQL type " + sqlType + " for JVM type " + javaType);
    }

    @Test
    void checksEveryField() {
        var compilation = compile("""
                @DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) String first;
                @DatabaseSerializable(type = DatabaseEncoders.Defaults.INT) int fine;
                @DatabaseSerializable(type = DatabaseEncoders.Defaults.DOUBLE) String second;
                """);

        assertThat(compilation).hadErrorCount(2);
    }
}
