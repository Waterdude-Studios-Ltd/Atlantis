package de.waterdu.atlantis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the files that CLAUDE.md says must be kept in sync by hand.
 */
class ProjectFilesConsistencyTest {

    private static final Path ROOT = findRoot();
    private static final Path COMMON_RESOURCES = ROOT.resolve("common/src/main/resources");
    private static final Pattern LOOM_TYPE_VARIABLE = Pattern.compile("T([A-Za-z0-9_$]+);");

    /**
     * Checks that both {@code fabric.mod.json} files inject the same interfaces as {@code common/interfaces.json}.
     *
     * @throws IOException if a file cannot be read
     */
    @Test
    void interfaceInjectionsAgree() throws IOException {
        var neoForge = normalise(JsonParser.parseString(Files.readString(ROOT.resolve("common/interfaces.json"))).getAsJsonObject());
        var commonFabric = normalise(loomInjections(COMMON_RESOURCES.resolve("fabric.mod.json")));
        var fabric = normalise(loomInjections(ROOT.resolve("fabric/src/main/resources/fabric.mod.json")));

        assertThat(neoForge).isNotEmpty();
        assertThat(commonFabric).as("common/src/main/resources/fabric.mod.json").isEqualTo(neoForge);
        assertThat(fabric).as("fabric/src/main/resources/fabric.mod.json").isEqualTo(neoForge);
    }

    /**
     * Checks that each interface in {@code common/interfaces.json} has a source file in common.
     *
     * @throws IOException if a file cannot be read
     */
    @Test
    void injectedInterfacesExist() throws IOException {
        var injections = normalise(JsonParser.parseString(Files.readString(ROOT.resolve("common/interfaces.json"))).getAsJsonObject());
        for (var interfaces : injections.values()) {
            for (var name : interfaces) {
                var erased = name.replaceAll("<.*>", "");
                assertThat(ROOT.resolve("common/src/main/java/" + erased + ".java")).as(name).exists();
            }
        }
    }

    /**
     * Checks that the access transformer and the access widener open the same classes, methods and fields.
     *
     * @throws IOException if a file cannot be read
     */
    @Test
    void accessTransformerMatchesAccessWidener() throws IOException {
        var transformer = new TreeSet<String>();
        for (var line : Files.readAllLines(COMMON_RESOURCES.resolve("META-INF/accesstransformer.cfg"))) {
            var entry = line.replaceAll("#.*", "").trim();
            if (entry.isEmpty()) {
                continue;
            }
            var parts = entry.split("\\s+", 3);
            var modifier = parts[0];
            var owner = parts[1].replace('.', '/');
            if (parts.length == 2) {
                transformer.add(access(modifier) + " class " + owner);
            } else if (parts[2].contains("(")) {
                var name = parts[2].substring(0, parts[2].indexOf('('));
                transformer.add(access(modifier) + " method " + owner + " " + name + " " + parts[2].substring(name.length()));
            } else {
                transformer.add(access(modifier) + " field " + owner + " " + parts[2]);
            }
        }

        var widener = new TreeSet<String>();
        var lines = Files.readAllLines(COMMON_RESOURCES.resolve("atlantis.accesswidener"));
        assertThat(lines.getFirst()).startsWith("accessWidener v2 named");
        for (var line : lines.subList(1, lines.size())) {
            var entry = line.replaceAll("#.*", "").trim();
            if (entry.isEmpty()) {
                continue;
            }
            var parts = entry.split("\\s+");
            switch (parts[1]) {
                case "class" -> widener.add(parts[0] + " class " + parts[2]);
                case "method" -> widener.add(parts[0] + " method " + parts[2] + " " + parts[3] + " " + parts[4]);
                case "field" -> widener.add(parts[0] + " field " + parts[2] + " " + parts[3]);
                default -> throw new AssertionError("Unknown access widener entry: " + line);
            }
        }

        assertThat(widener).isEqualTo(transformer);
    }

    /**
     * Checks that each mixin class is listed once in {@code atlantis.mixins.json}, under {@code client} if it imports
     * client classes and under {@code mixins} if not. Also checks that every injector is required.
     *
     * @throws IOException if a file cannot be read
     */
    @Test
    void everyMixinIsRegisteredInTheRightSection() throws IOException {
        var config = JsonParser.parseString(Files.readString(COMMON_RESOURCES.resolve("atlantis.mixins.json"))).getAsJsonObject();
        var packageName = config.get("package").getAsString();
        var common = names(config, "mixins");
        var client = names(config, "client");
        var server = names(config, "server");

        var listed = new ArrayList<String>();
        listed.addAll(common);
        listed.addAll(client);
        listed.addAll(server);
        assertThat(listed).as("mixins listed more than once").doesNotHaveDuplicates();

        var mixinRoot = ROOT.resolve("common/src/main/java/" + packageName.replace('.', '/'));
        var expectedCommon = new TreeSet<String>();
        var expectedClient = new TreeSet<String>();
        try (var files = Files.walk(mixinRoot)) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                var relative = mixinRoot.relativize(file).toString();
                var name = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
                if (Files.readString(file).contains("import net.minecraft.client.")) {
                    expectedClient.add(name);
                } else {
                    expectedCommon.add(name);
                }
            }
        }

        assertThat(new TreeSet<>(common)).as("common mixins").isEqualTo(expectedCommon);
        assertThat(new TreeSet<>(client)).as("client mixins").isEqualTo(expectedClient);
        assertThat(server).as("server mixins").isEmpty();
        assertThat(config.getAsJsonObject("injectors").get("defaultRequire").getAsInt()).isEqualTo(1);
    }

    /**
     * Converts an access transformer modifier to the matching access widener access.
     *
     * @param transformerModifier {@code public} or {@code public-f}
     * @return {@code accessible} or {@code mutable}
     */
    private static String access(String transformerModifier) {
        return switch (transformerModifier) {
            case "public" -> "accessible";
            case "public-f" -> "mutable";
            default -> throw new AssertionError("Unsupported access transformer modifier: " + transformerModifier);
        };
    }

    /**
     * @param config the mixin config
     * @param section the name of the array to read, such as {@code mixins}
     * @return the mixin names in the section, or an empty list if the section is missing
     */
    private static List<String> names(JsonObject config, String section) {
        var array = config.getAsJsonArray(section);
        var names = new ArrayList<String>();
        if (array != null) {
            array.forEach(element -> names.add(element.getAsString()));
        }
        return names;
    }

    /**
     * @param fabricModJson the {@code fabric.mod.json} file to read
     * @return the {@code loom:injected_interfaces} object under {@code custom}
     * @throws IOException if the file cannot be read
     */
    private static JsonObject loomInjections(Path fabricModJson) throws IOException {
        var json = JsonParser.parseString(Files.readString(fabricModJson)).getAsJsonObject();
        return json.getAsJsonObject("custom").getAsJsonObject("loom:injected_interfaces");
    }

    /**
     * Converts injections to sorted sets, so that their order does not matter. Loom type variables such as
     * {@code TT;} become plain names.
     *
     * @param injections the injections, with target classes as keys and arrays of interfaces as values
     * @return the sorted interfaces of each target class
     */
    private static Map<String, Set<String>> normalise(JsonObject injections) {
        var result = new TreeMap<String, Set<String>>();
        for (var entry : injections.entrySet()) {
            var interfaces = new TreeSet<String>();
            entry.getValue().getAsJsonArray().forEach(element -> interfaces.add(LOOM_TYPE_VARIABLE.matcher(element.getAsString()).replaceAll("$1")));
            result.put(entry.getKey(), interfaces);
        }
        return result;
    }

    /**
     * Finds the repository root. Starts at the working directory and goes up until it finds
     * {@code common/interfaces.json}.
     *
     * @return the repository root
     * @throws IllegalStateException if no parent directory holds the file
     */
    private static Path findRoot() {
        var dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("common/interfaces.json"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("Could not find the repository root from " + Path.of("").toAbsolutePath());
        }
        return dir;
    }
}
