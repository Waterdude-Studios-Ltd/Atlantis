package de.waterdu.atlantis.trident;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.version.VersionRange;
import io.github.classgraph.ClassGraph;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Due to the NeoForge/Fabric proxies present within Atlantis, mods that depend on Atlantis don't strictly need to know
 * about either launcher.
 * As such, we want to manage the loading of such mods ourselves.
 * This loading functionality is called "Trident", with an individual mod a "Tine".
 * These JARs should be placed within "./config/atlantis/trident" and describe themselves with a
 * {@value TineMetadata#FILE_NAME}.
 * These are loaded before anything else, so they can still register lifecycle listeners via loader proxies and
 * set up anything they need to. Their {@link Bootstrapped} and config classes are discovered alongside Atlantis' own.
 * They are loaded onto Atlantis' classloader where the modloader allows it (Fabric), or otherwise a shared child
 * classloader that Atlantis and other mods can still resolve classes from (NeoForge), and are accessible to one another.
 */
@Bootstrapped(order = -50)
public final class TridentLoader {

    public static final String DIRECTORY = "atlantis/trident";

    private static volatile Map<String, TineContainer> tines = Map.of();
    private static volatile ClassLoader classLoader = TridentLoader.class.getClassLoader();

    private TridentLoader() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    @VisibleForTesting
    public static void resetForTesting() {
        tines = Map.of();
        classLoader = TridentLoader.class.getClassLoader();
    }

    public static boolean isLoaded(String id) {
        return tines.containsKey(id);
    }

    public static Collection<TineContainer> tines() {
        return tines.values();
    }

    public static List<ModInfo> mods() {
        return tines.values().stream().map(TineContainer::toModInfo).toList();
    }

    public static ClassLoader classLoader() {
        return classLoader;
    }

    public static Discovery discover(ModLoaderDelegate loader, Collection<Class<? extends Annotation>> annotations) {
        var candidates = TridentLoader.read(loader, TridentLoader.jars(loader.configDir().resolve(DIRECTORY)), annotations);
        var ordered = TridentLoader.resolve(loader, candidates);
        if (ordered.isEmpty()) {
            return new Discovery(classLoader, Map.of());
        }

        try {
            classLoader = loader.addToClassPath(ordered.stream().map(candidate -> candidate.container().path()).toList());
        } catch (RuntimeException e) {
            AtlantisLogger.error("Failed to add Tines to the classpath", e);
            return new Discovery(classLoader, Map.of());
        }

        var containers = new LinkedHashMap<String, TineContainer>();
        var classes = new HashMap<Class<? extends Annotation>, List<String>>();
        for (var candidate : ordered) {
            containers.put(candidate.container().id(), candidate.container());
            candidate.classes().forEach((annotation, names) -> classes.computeIfAbsent(annotation, __ -> new ArrayList<>()).addAll(names));
        }

        tines = Collections.unmodifiableMap(containers);

        AtlantisLogger.info("Loaded {} Tine(s): {}", containers.size(), containers.values().stream()
                .map(container -> container.id() + " " + container.metadata().version())
                .collect(Collectors.joining(", ")));

        return new Discovery(classLoader, classes);
    }

    @Entrypoint
    public static void barb() {
        for (var tine : tines.values()) {
            var mainClass = tine.mainClass();
            if (mainClass == null) {
                continue;
            }

            try {
                Bootstrapper.invokeStatic(Class.forName(mainClass, false, classLoader), Tine.Barb.class);
            } catch (ClassNotFoundException | LinkageError e) {
                AtlantisLogger.error("Failed to load main class " + mainClass + " of Tine " + tine.id(), e);
            }
        }
    }

    private static List<Path> jars(Path directory) {
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                return files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")).sorted().toList();
            }
        } catch (IOException e) {
            AtlantisLogger.error("Failed to list Tines in " + directory, e);
            return List.of();
        }
    }

    public static URL[] jarsToURLs(List<Path> jars) {
        var urls = new URL[jars.size()];
        for (var i = 0; i < urls.length; i++) {
            try {
                urls[i] = jars.get(i).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }
        return urls;
    }

    private static Map<String, Candidate> read(ModLoaderDelegate loader, List<Path> jars, Collection<Class<? extends Annotation>> annotations) {
        var candidates = new LinkedHashMap<String, Candidate>();
        var duplicates = new ArrayList<String>();

        for (var jar : jars) {
            var candidate = TridentLoader.scan(jar, annotations);
            if (candidate == null) {
                continue;
            }

            var metadata = candidate.container().metadata();
            if (loader.isModLoaded(metadata.id())) {
                AtlantisLogger.error("Skipping Tine {} ({}), a mod with that ID is already loaded", metadata.id(), jar.getFileName());
            } else if (!metadata.atlantisRange().test(Atlantis.VERSION)) {
                AtlantisLogger.error("Skipping Tine {} ({}), it requires Atlantis {} but {} is installed", metadata.id(), jar.getFileName(), metadata.atlantis(), Atlantis.VERSION);
            } else if (candidates.putIfAbsent(metadata.id(), candidate) != null) {
                duplicates.add(metadata.id());
            }
        }

        for (var id : duplicates) {
            if (candidates.remove(id) != null) {
                AtlantisLogger.error("Skipping Tine {}, more than one jar declares that ID", id);
            }
        }

        return candidates;
    }

    private static @Nullable Candidate scan(Path jar, Collection<Class<? extends Annotation>> annotations) {
        try (var scan = new ClassGraph().overrideClasspath(jar).enableClassInfo().enableAnnotationInfo().ignoreClassVisibility().scan()) {
            var resources = scan.getResourcesWithPath(TineMetadata.FILE_NAME);
            if (resources.isEmpty()) {
                AtlantisLogger.warn("Skipping {}, it has no {}", jar.getFileName(), TineMetadata.FILE_NAME);
                return null;
            }

            var metadata = TineMetadata.parse(resources.getFirst().getContentAsString());
            var mainClasses = scan.getClassesWithAnnotation(Tine.class.getName()).getNames();
            if (mainClasses.size() > 1) {
                AtlantisLogger.error("Skipping Tine {} ({}), it has more than one @Tine class: {}", metadata.id(), jar.getFileName(), mainClasses);
                return null;
            }

            var classes = new HashMap<Class<? extends Annotation>, List<String>>();
            for (var annotation : annotations) {
                classes.put(annotation, scan.getClassesWithAnnotation(annotation.getName()).getNames());
            }

            return new Candidate(new TineContainer(metadata, jar, mainClasses.isEmpty() ? null : mainClasses.getFirst()), classes);
        } catch (IOException | RuntimeException e) {
            AtlantisLogger.error("Skipping " + jar.getFileName() + ", it could not be read", e);
            return null;
        }
    }

    private static List<Candidate> resolve(ModLoaderDelegate loader, Map<String, Candidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        var versions = new HashMap<String, String>();
        for (var mod : loader.mods()) {
            versions.put(mod.id(), mod.version());
        }

        var remaining = new LinkedHashMap<>(candidates);
        var changed = true;
        while (changed) {
            changed = false;
            for (var iterator = remaining.values().iterator(); iterator.hasNext(); ) {
                var metadata = iterator.next().container().metadata();
                var problem = TridentLoader.findUnmetDependency(loader, metadata, remaining, versions);
                if (problem != null) {
                    AtlantisLogger.error("Skipping Tine {}, {}", metadata.id(), problem);
                    iterator.remove();
                    changed = true;
                }
            }
        }

        var waitingOn = new HashMap<String, Integer>();
        var dependents = new HashMap<String, List<String>>();
        var ready = new PriorityQueue<>(Comparator
                .comparingInt((Candidate candidate) -> candidate.container().metadata().order())
                .thenComparing(candidate -> candidate.container().id()));
        for (var candidate : remaining.values()) {
            var id = candidate.container().id();
            var count = 0;
            for (var dependency : candidate.container().metadata().depends().keySet()) {
                if (remaining.containsKey(dependency)) {
                    dependents.computeIfAbsent(dependency, __ -> new ArrayList<>()).add(id);
                    count++;
                }
            }
            waitingOn.put(id, count);
            if (count == 0) {
                ready.add(candidate);
            }
        }

        var ordered = new ArrayList<Candidate>();
        while (!ready.isEmpty()) {
            var candidate = ready.poll();
            ordered.add(candidate);
            for (var dependent : dependents.getOrDefault(candidate.container().id(), List.of())) {
                if (waitingOn.merge(dependent, -1, Integer::sum) == 0) {
                    ready.add(remaining.get(dependent));
                }
            }
        }

        if (ordered.size() < remaining.size()) {
            for (var id : remaining.keySet()) {
                if (waitingOn.get(id) > 0) {
                    AtlantisLogger.error("Skipping Tine {}, it is part of or depends on a dependency cycle", id);
                }
            }
        }

        return ordered;
    }

    private static @Nullable String findUnmetDependency(ModLoaderDelegate loader, TineMetadata metadata, Map<String, Candidate> tines, Map<String, String> versions) {
        for (var dependency : metadata.depends().entrySet()) {
            var id = dependency.getKey();
            var range = VersionRange.parse(dependency.getValue());
            var tine = tines.get(id);
            var version = tine != null ? tine.container().metadata().version() : versions.get(id);
            if (version == null) {
                if (range.isAny() && loader.isModLoaded(id)) {
                    continue;
                }
                return "it depends on " + id + " " + dependency.getValue() + " which is not installed";
            }

            if (!range.test(version)) {
                return "it depends on " + id + " " + dependency.getValue() + " but " + version + " is installed";
            }
        }
        return null;
    }

    public record Discovery(ClassLoader classLoader, Map<Class<? extends Annotation>, List<String>> classes) {}

    private record Candidate(TineContainer container, Map<Class<? extends Annotation>, List<String>> classes) {}
}
