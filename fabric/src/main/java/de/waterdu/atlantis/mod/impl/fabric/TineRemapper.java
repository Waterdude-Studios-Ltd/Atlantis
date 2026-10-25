package de.waterdu.atlantis.mod.impl.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.trident.TridentLoader;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tines are compiled against Mojang's names, which Fabric only uses in development. In production, Tine jars are
 * remapped to intermediary before they are put on the classpath, and the results are cached by content hash in
 * {@value #CACHE}. Only bytecode references are remapped, so class and member names in strings or reflection won't
 * resolve in production. The Mojang mappings are downloaded once per Minecraft version, or read from the file in the
 * {@value #MAPPINGS_PROPERTY} system property.
 */
final class TineRemapper {

    static final String CACHE = TridentLoader.DIRECTORY + "/.cache";
    static final String MAPPINGS_PROPERTY = "atlantis.trident.mappings";

    private static final String NAMED = "named";
    private static final String OFFICIAL = "official";
    private static final String INTERMEDIARY = "intermediary";
    private static final String INTERMEDIARY_MAPPINGS = "mappings/mappings.tiny";
    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final int FORMAT = 1;

    private TineRemapper() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    static List<Path> remap(List<Path> paths, Path configDir) {
        var namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
        if (NAMED.equals(namespace)) {
            return paths;
        }
        if (!INTERMEDIARY.equals(namespace)) {
            AtlantisLogger.warn("Not remapping Tines, the runtime namespace is {}", namespace);
            return paths;
        }

        try {
            return TineRemapper.remapToIntermediary(paths, configDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to remap Tines to intermediary", e);
        }
    }

    private static List<Path> remapToIntermediary(List<Path> paths, Path configDir) throws IOException {
        var minecraft = TineRemapper.minecraftVersion();
        var cache = Files.createDirectories(configDir.resolve(CACHE));
        var outputs = new LinkedHashMap<Path, Path>();
        var pending = new LinkedHashMap<Path, Path>();
        for (var path : paths) {
            if (Files.isDirectory(path)) {
                outputs.put(path, path);
                continue;
            }
            var output = cache.resolve("tines").resolve(TineRemapper.sha256(path, minecraft) + ".jar");
            outputs.put(path, output);
            if (!Files.isRegularFile(output)) {
                pending.put(path, output);
            }
        }

        if (!pending.isEmpty()) {
            AtlantisLogger.info("Remapping {} Tine(s) to intermediary", pending.size());
            var mappings = TineRemapper.mappings(cache, minecraft);
            var classpath = TineRemapper.namedClassPath(cache, minecraft, mappings);
            TineRemapper.run(mappings, NAMED, INTERMEDIARY, pending, classpath);
        }

        return List.copyOf(outputs.values());
    }

    private static List<Path> namedClassPath(Path cache, String minecraft, MemoryMappingTree mappings) throws IOException {
        var intermediary = new LinkedHashMap<Path, Path>();
        var sources = new HashSet<Path>();
        for (var mod : List.of("minecraft", Atlantis.MOD_ID)) {
            var container = FabricLoader.getInstance().getModContainer(mod).orElseThrow();
            var version = container.getMetadata().getVersion().getFriendlyString();
            for (var path : container.getOrigin().getPaths()) {
                sources.add(path.toAbsolutePath());
                var output = cache.resolve("classpath").resolve(mod + "-" + version + "-" + TineRemapper.sha256(path, minecraft) + ".jar");
                intermediary.put(path, output);
            }
        }

        var pending = new LinkedHashMap<Path, Path>();
        intermediary.forEach((input, output) -> {
            if (!Files.isRegularFile(output)) {
                pending.put(input, output);
            }
        });

        var libraries = new ArrayList<Path>();
        for (var path : FabricLauncherBase.getLauncher().getClassPath()) {
            if (!sources.contains(path.toAbsolutePath()) && Files.exists(path)) {
                libraries.add(path);
            }
        }

        if (!pending.isEmpty()) {
            TineRemapper.run(mappings, INTERMEDIARY, NAMED, pending, libraries);
        }

        var classpath = new ArrayList<>(intermediary.values());
        classpath.addAll(libraries);
        return classpath;
    }

    private static void run(MemoryMappingTree mappings, String from, String to, Map<Path, Path> jars, List<Path> classpath) throws IOException {
        var remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createMappingProvider(mappings, from, to))
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .build();
        try {
            remapper.readClassPath(classpath.toArray(Path[]::new));
            var tags = new LinkedHashMap<Path, InputTag>();
            for (var jar : jars.keySet()) {
                var tag = remapper.createInputTag();
                tags.put(jar, tag);
                remapper.readInputs(tag, jar);
            }

            for (var entry : jars.entrySet()) {
                var output = entry.getValue();
                Files.createDirectories(output.getParent());
                var temporary = output.resolveSibling(output.getFileName() + ".tmp");
                Files.deleteIfExists(temporary);
                try (var consumer = new OutputConsumerPath.Builder(temporary).assumeArchive(true).build()) {
                    consumer.addNonClassFiles(entry.getKey(), NonClassCopyMode.FIX_META_INF, remapper);
                    remapper.apply(consumer, tags.get(entry.getKey()));
                }
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        } finally {
            remapper.finish();
        }
    }

    private static MemoryMappingTree mappings(Path cache, String minecraft) throws IOException {
        var tree = new MemoryMappingTree();
        try (var reader = Files.newBufferedReader(TineRemapper.mojangMappings(cache, minecraft))) {
            ProGuardFileReader.read(reader, NAMED, OFFICIAL, new MappingSourceNsSwitch(tree, OFFICIAL));
        }

        var intermediary = TineRemapper.class.getClassLoader().getResourceAsStream(INTERMEDIARY_MAPPINGS);
        if (intermediary == null) {
            throw new IOException("Intermediary mappings (" + INTERMEDIARY_MAPPINGS + ") are not on the classpath");
        }
        try (var reader = new InputStreamReader(intermediary, StandardCharsets.UTF_8)) {
            MappingReader.read(reader, tree);
        }
        return tree;
    }

    private static Path mojangMappings(Path cache, String minecraft) throws IOException {
        var override = System.getProperty(MAPPINGS_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }

        var file = cache.resolve("mojmap-" + minecraft + ".txt");
        if (Files.isRegularFile(file)) {
            return file;
        }

        AtlantisLogger.info("Downloading Mojang mappings for Minecraft {}", minecraft);
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            var manifest = TineRemapper.json(client, VERSION_MANIFEST);
            String versionUrl = null;
            for (var version : manifest.getAsJsonArray("versions")) {
                var object = version.getAsJsonObject();
                if (minecraft.equals(object.get("id").getAsString())) {
                    versionUrl = object.get("url").getAsString();
                    break;
                }
            }
            if (versionUrl == null) {
                throw new IOException("Minecraft " + minecraft + " is not in Mojang's version manifest");
            }

            var download = TineRemapper.json(client, versionUrl).getAsJsonObject("downloads").getAsJsonObject("client_mappings");
            var bytes = client.send(HttpRequest.newBuilder(URI.create(download.get("url").getAsString())).build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            var expected = download.get("sha1").getAsString();
            var actual = TineRemapper.hex("SHA-1", bytes);
            if (!expected.equalsIgnoreCase(actual)) {
                throw new IOException("Mojang mappings for " + minecraft + " have SHA-1 " + actual + ", expected " + expected);
            }

            var temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temporary, bytes);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return file;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading Mojang mappings", e);
        }
    }

    private static JsonObject json(HttpClient client, String url) throws IOException, InterruptedException {
        var response = client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GET " + url + " returned " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    private static String sha256(Path jar, String minecraft) throws IOException {
        var digest = TineRemapper.digest("SHA-256");
        digest.update((FORMAT + ":" + minecraft + ":" + Atlantis.VERSION + ":").getBytes(StandardCharsets.UTF_8));
        try (var input = new DigestInputStream(Files.newInputStream(jar), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String hex(String algorithm, byte[] bytes) {
        return HexFormat.of().formatHex(TineRemapper.digest(algorithm).digest(bytes));
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
