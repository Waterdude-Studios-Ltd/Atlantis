package de.waterdu.atlantis.trident;

import com.google.testing.compile.JavaFileObjects;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.TestModLoader;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AtlantisTestEnvironment.class)
public class TridentLoaderTest {

    public static final List<String> CALLS = Collections.synchronizedList(new ArrayList<>());

    private static final String MAIN = """
            package %1$s;

            @de.waterdu.atlantis.trident.Tine
            public class Main {
                @de.waterdu.atlantis.trident.Tine.Barb
                static void barb() {
                    de.waterdu.atlantis.trident.TridentLoaderTest.CALLS.add("%1$s");
                }
            }
            """;

    @BeforeEach
    void clearCalls() {
        CALLS.clear();
    }

    @Test
    void runsBarbsBeforeOtherEntrypoints(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0"}
                """, compile(Map.of(
                "a.Main", MAIN.formatted("a"),
                "a.Setup", """
                        package a;

                        @de.waterdu.atlantis.util.bootstrap.Bootstrapped
                        public class Setup {
                            @de.waterdu.atlantis.util.bootstrap.Entrypoint
                            static void setup() {
                                de.waterdu.atlantis.trident.TridentLoaderTest.CALLS.add("a.setup");
                            }
                        }
                        """
        )));

        bootstrap(loader);

        assertThat(CALLS).containsExactly("a", "a.setup");
        assertThat(TridentLoader.isLoaded("a")).isTrue();
    }

    @Test
    void loadsDependenciesFirstAndSharesClasses(TestModLoader loader) throws IOException {
        var classes = compile(Map.of(
                "a.Main", MAIN.formatted("a"),
                "a.Api", """
                        package a;

                        public class Api {
                            public static String greet() {
                                return "hello from a";
                            }
                        }
                        """,
                "b.Main", """
                        package b;

                        @de.waterdu.atlantis.trident.Tine
                        public class Main {
                            @de.waterdu.atlantis.trident.Tine.Barb
                            static void barb() {
                                de.waterdu.atlantis.trident.TridentLoaderTest.CALLS.add(a.Api.greet());
                            }
                        }
                        """
        ));
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.2.0", "order": 10}
                """, only(classes, "a/"));
        jar(loader, "b.jar", """
                {"id": "b", "version": "1.0.0", "order": -10, "depends": {"a": "^1.0.0"}}
                """, only(classes, "b/"));

        bootstrap(loader);

        assertThat(CALLS).containsExactly("a", "hello from a");
        assertThat(TridentLoader.tines()).extracting(TineContainer::id).containsExactly("a", "b");
    }

    @Test
    void ordersIndependentTinesByOrderThenId(TestModLoader loader) throws IOException {
        var classes = compile(Map.of("a.Main", MAIN.formatted("a"), "b.Main", MAIN.formatted("b"), "c.Main", MAIN.formatted("c")));
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0", "order": 5}
                """, only(classes, "a/"));
        jar(loader, "b.jar", """
                {"id": "b", "version": "1.0.0"}
                """, only(classes, "b/"));
        jar(loader, "c.jar", """
                {"id": "c", "version": "1.0.0", "order": -5}
                """, only(classes, "c/"));

        bootstrap(loader);

        assertThat(CALLS).containsExactly("c", "b", "a");
    }

    @Test
    void skipsTinesForOtherAtlantisVersions(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0", "atlantis": "<1.0.0"}
                """, compile(Map.of("a.Main", MAIN.formatted("a"))));

        bootstrap(loader);

        assertThat(CALLS).isEmpty();
        assertThat(TridentLoader.isLoaded("a")).isFalse();
    }

    @Test
    void skipsUnmetDependenciesAndTheirDependents(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0"}
                """, Map.of());
        jar(loader, "b.jar", """
                {"id": "b", "version": "1.0.0", "depends": {"a": ">=2.0.0"}}
                """, Map.of());
        jar(loader, "c.jar", """
                {"id": "c", "version": "1.0.0", "depends": {"b": "*"}}
                """, Map.of());
        jar(loader, "d.jar", """
                {"id": "d", "version": "1.0.0", "depends": {"missing": "*"}}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.tines()).extracting(TineContainer::id).containsExactly("a");
    }

    @Test
    void skipsDependencyCycles(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0", "depends": {"b": "*"}}
                """, Map.of());
        jar(loader, "b.jar", """
                {"id": "b", "version": "1.0.0", "depends": {"a": "*"}}
                """, Map.of());
        jar(loader, "c.jar", """
                {"id": "c", "version": "1.0.0"}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.tines()).extracting(TineContainer::id).containsExactly("c");
    }

    @Test
    void resolvesModloaderModDependencies(TestModLoader loader) throws IOException {
        loader.loadMod("cobblemon");
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0", "depends": {"cobblemon": "*"}}
                """, Map.of());
        jar(loader, "b.jar", """
                {"id": "b", "version": "1.0.0", "depends": {"cobblemon": ">=1.0.0"}}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.tines()).extracting(TineContainer::id).containsExactly("a");
    }

    @Test
    void skipsDuplicateAndClashingIds(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "dup", "version": "1.0.0"}
                """, Map.of());
        jar(loader, "b.jar", """
                {"id": "dup", "version": "2.0.0"}
                """, Map.of());
        jar(loader, "c.jar", """
                {"id": "atlantis", "version": "1.0.0"}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.tines()).isEmpty();
    }

    @Test
    void skipsInvalidJars(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", null, Map.of());
        jar(loader, "b.jar", """
                {"id": "Not Valid", "version": "1.0.0"}
                """, Map.of());
        jar(loader, "c.jar", """
                {"id": "c"}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.tines()).isEmpty();
    }

    @Test
    void exposesTinesAsMods(TestModLoader loader) throws IOException {
        jar(loader, "a.jar", """
                {"id": "a", "version": "1.0.0", "name": "Alpha", "authors": ["Waterdude"], "issues": "https://example.org"}
                """, Map.of());

        bootstrap(loader);

        assertThat(TridentLoader.mods()).singleElement().satisfies(mod -> {
            assertThat(mod.id()).isEqualTo("a");
            assertThat(mod.displayName()).isEqualTo("Alpha");
            assertThat(mod.version()).isEqualTo("1.0.0");
            assertThat(mod.authors()).containsExactly("Waterdude");
            assertThat(mod.issueTracker()).isEqualTo("https://example.org");
        });
    }

    private static void bootstrap(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, TridentLoader.class);
        Bootstrapper.bootstrap(loader);
    }

    private static Map<String, byte[]> compile(Map<String, String> sources) throws IOException {
        var compilation = javac().compile(sources.entrySet().stream()
                .map(source -> JavaFileObjects.forSourceString(source.getKey(), source.getValue()))
                .toList());
        assertThat(compilation).succeeded();
        var classes = new HashMap<String, byte[]>();
        for (var file : compilation.generatedFiles()) {
            if (file.getKind() == JavaFileObject.Kind.CLASS) {
                var path = file.toUri().getPath();
                try (var input = file.openInputStream()) {
                    classes.put(path.substring(path.indexOf("CLASS_OUTPUT/") + "CLASS_OUTPUT/".length()), input.readAllBytes());
                }
            }
        }
        return classes;
    }

    private static Map<String, byte[]> only(Map<String, byte[]> classes, String prefix) {
        var filtered = new HashMap<String, byte[]>();
        classes.forEach((path, bytes) -> {
            if (path.startsWith(prefix)) {
                filtered.put(path, bytes);
            }
        });
        return filtered;
    }

    private static void jar(TestModLoader loader, String name, String metadata, Map<String, byte[]> classes) throws IOException {
        var directory = Files.createDirectories(loader.configDir().resolve(TridentLoader.DIRECTORY));
        try (var output = new JarOutputStream(Files.newOutputStream(directory.resolve(name)))) {
            if (metadata != null) {
                output.putNextEntry(new JarEntry(TineMetadata.FILE_NAME));
                output.write(metadata.getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
            for (var entry : classes.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
    }
}
