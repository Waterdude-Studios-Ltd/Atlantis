package de.waterdu.atlantis.trident;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.version.VersionRange;
import io.github.classgraph.ClassGraph;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Atlantis has NeoForge and Fabric proxies. Thus, mods that depend on Atlantis do not strictly need to know about
 * either launcher.
 * For this reason, Atlantis manages the loading of these mods itself.
 * This loading system is called "Trident", and an individual mod is called a "Tine".
 * Put these JARs in "./config/atlantis/trident". Each JAR describes itself with a {@value TineMetadata#FILE_NAME}.
 * Trident loads them before anything else. Thus, they can still register lifecycle listeners through loader proxies
 * and set up everything they need. Their {@link Bootstrapped} and config classes are discovered together with the
 * classes of Atlantis.
 * Where the mod loader allows it (Fabric), Trident loads them onto the classloader of Atlantis. Otherwise (NeoForge),
 * Trident loads them onto a shared child classloader. Atlantis and other mods can still resolve classes from that
 * classloader. Tines can access one another.
 * When the jar of a Tine is on the classpath, Trident registers the mixin configs that its
 * {@value TineMetadata#FILE_NAME} lists.
 * Fabric discovers Tines at preLaunch, so their mixins can target nearly any class. NeoForge can only discover them
 * during mod construction, after much of the game has loaded.
 * During development, Trident can load one more Tine from the paths in the {@value #DEV_PROPERTY} system property.
 * The paths are joined by {@link File#pathSeparator}. The Tine Gradle plugin sets this property on its runs.
 */
@Bootstrapped(Bootstrapped.Priority.VERY_HIGH)
public final class Trident {

    public static final String DIRECTORY = "atlantis/trident";
    public static final String DEV_PROPERTY = "atlantis.trident.dev";

    private static volatile Map<String, TineContainer> tines = Map.of();
    private static volatile ClassLoader classLoader = Trident.class.getClassLoader();
    private static volatile @Nullable Discovery discovery;

    private Trident() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Clears the loaded Tines, the class loader and the discovery result, so that discovery can run again.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        tines = Map.of();
        classLoader = Trident.class.getClassLoader();
        discovery = null;
    }

    /**
     * Checks if a Tine with the ID is loaded.
     *
     * @param id the ID of the Tine
     * @return {@code true} if the Tine is loaded
     */
    public static boolean isLoaded(String id) {
        return tines.containsKey(id);
    }

    /**
     * @return the loaded Tines in load order. This is empty before {@link #discover(TridentHost, Collection)}.
     */
    public static Collection<TineContainer> tines() {
        return tines.values();
    }

    /**
     * @return the mod info of the loaded Tines in load order
     */
    public static List<ModInfo> mods() {
        return tines.values().stream().map(TineContainer::toModInfo).toList();
    }

    /**
     * The class loader that loads Tine classes. Before discovery, or if no Tines load, this is the class loader of
     * Atlantis.
     */
    public static ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * Finds, validates and orders the Tines, adds them to the class path and registers their mixin configs. Only the
     * first call does this. Later calls return the first result and ignore their arguments.
     *
     * @param annotations the annotations to look for on the Tine classes
     * @return the class loader and the names of the Tine classes that have each annotation
     */
    public static synchronized Discovery discover(TridentHost host, Collection<Class<? extends Annotation>> annotations) {
        if (discovery == null) {
            discovery = Trident.find(host, annotations);
        }
        return discovery;
    }

    /**
     * Reads the Tine jars and the dev Tine, orders them, adds them to the class path and registers their mixin
     * configs. If no Tine loads or the class path change fails, the result has no classes.
     *
     * @param loader the host that gives the config directory, the loaded mods and the class path
     * @param annotations the annotations to look for on the Tine classes
     * @return the class loader and the names of the Tine classes that have each annotation
     */
    private static Discovery find(TridentHost loader, Collection<Class<? extends Annotation>> annotations) {
        TridentLogger.info("Starting Tine discovery");

        var sources = new ArrayList<List<Path>>();
        Trident.jars(loader.configDir().resolve(DIRECTORY)).forEach(jar -> sources.add(List.of(jar)));
        Trident.devTine().ifPresent(sources::add);
        var candidates = Trident.read(loader, sources, annotations);
        var ordered = Trident.resolve(loader, candidates);
        if (ordered.isEmpty()) {
            TridentLogger.info("No Tines found");
            return new Discovery(classLoader, Map.of());
        }

        try {
            classLoader = loader.addToClassPath(ordered.stream().flatMap(candidate -> candidate.container().paths().stream()).toList());
        } catch (RuntimeException e) {
            TridentLogger.error("Failed to add Tines to the classpath", e);
            return new Discovery(classLoader, Map.of());
        }

        var containers = new LinkedHashMap<String, TineContainer>();
        var classes = new HashMap<Class<? extends Annotation>, List<String>>();
        for (var candidate : ordered) {
            containers.put(candidate.container().id(), candidate.container());
            candidate.classes().forEach((annotation, names) -> classes.computeIfAbsent(annotation, __ -> new ArrayList<>()).addAll(names));
        }

        tines = Collections.unmodifiableMap(containers);
        TineMixins.register(containers.values());

        TridentLogger.info("Loaded {} Tine(s): {}", containers.size(), containers.values().stream()
                .map(container -> container.id() + " " + container.metadata().version())
                .collect(Collectors.joining(", ")));

        return new Discovery(classLoader, classes);
    }

    /**
     * Calls the {@link Tine.Barb} method of the main class of each loaded Tine, in load order. It logs an error if
     * a main class cannot be loaded.
     */
    @Entrypoint
    public static void load() {
        TridentLogger.info("Loading {} Tine(s)", tines.size());

        for (var tine : tines.values()) {
            var mainClass = tine.mainClass();
            if (mainClass == null) {
                continue;
            }

            try {
                Bootstrapper.invokeStatic(Class.forName(mainClass, false, classLoader), Tine.Barb.class);
            } catch (ClassNotFoundException | LinkageError e) {
                TridentLogger.error("Failed to load main class " + mainClass + " of Tine " + tine.id(), e);
            }
        }
    }

    /**
     * Lists the jar files in the directory, sorted by path. It creates the directory if it does not exist.
     *
     * @param directory the directory to list
     * @return the jar files, or an empty list if the directory cannot be read
     */
    private static List<Path> jars(Path directory) {
        try {
            Files.createDirectories(directory);
            try (var files = Files.list(directory)) {
                return files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")).sorted().toList();
            }
        } catch (IOException e) {
            TridentLogger.error("Failed to list Tines in " + directory, e);
            return List.of();
        }
    }

    /**
     * Gets the paths of the dev Tine from the {@value #DEV_PROPERTY} system property. Paths that do not exist are
     * left out.
     *
     * @return the paths, or an empty optional if the property is not set or no path exists
     */
    private static Optional<List<Path>> devTine() {
        var property = System.getProperty(DEV_PROPERTY);
        if (property == null || property.isBlank()) {
            return Optional.empty();
        }

        var paths = Arrays.stream(property.split(Pattern.quote(File.pathSeparator)))
                .filter(path -> !path.isBlank())
                .map(Path::of)
                .filter(Files::exists)
                .toList();
        return paths.isEmpty() ? Optional.empty() : Optional.of(paths);
    }

    /**
     * Converts the paths to URLs.
     *
     * @param jars the paths of the jars and directories
     * @return the URLs in the same order
     * @throws UncheckedIOException if a path cannot be converted to a URL
     */
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

    /**
     * Scans each source and keeps the valid Tines. It skips a Tine if a mod with its ID is loaded, if it does not
     * support this Atlantis version, or if more than one source declares its ID.
     *
     * @param loader the host that tells which mods are loaded
     * @param sources the paths of each Tine. The first path names the Tine in the log.
     * @param annotations the annotations to look for on the Tine classes
     * @return the candidates by Tine ID, in source order
     */
    private static Map<String, Candidate> read(TridentHost loader, List<List<Path>> sources, Collection<Class<? extends Annotation>> annotations) {
        var candidates = new LinkedHashMap<String, Candidate>();
        var duplicates = new ArrayList<String>();

        for (var paths : sources) {
            var jar = paths.getFirst();
            var candidate = Trident.scan(paths, annotations);
            if (candidate == null) {
                continue;
            }

            var metadata = candidate.container().metadata();
            if (loader.isModLoaded(metadata.id())) {
                TridentLogger.error("Skipping Tine {} ({}), a mod with that ID is already loaded", metadata.id(), jar.getFileName());
            } else if (!metadata.atlantisRange().test(Atlantis.VERSION)) {
                TridentLogger.error("Skipping Tine {} ({}), it requires Atlantis {} but {} is installed", metadata.id(), jar.getFileName(), metadata.atlantis(), Atlantis.VERSION);
            } else if (candidates.putIfAbsent(metadata.id(), candidate) != null) {
                duplicates.add(metadata.id());
            }
        }

        for (var id : duplicates) {
            if (candidates.remove(id) != null) {
                TridentLogger.error("Skipping Tine {}, more than one jar declares that ID", id);
            }
        }

        return candidates;
    }

    /**
     * Scans the paths of a Tine with ClassGraph. It reads the {@value TineMetadata#FILE_NAME}, the {@link Tine}
     * class and the classes that have each annotation.
     *
     * @param paths the paths of the Tine
     * @param annotations the annotations to look for
     * @return the candidate, or {@code null} if the paths have no {@value TineMetadata#FILE_NAME}, more than one
     *     {@link Tine} class, or cannot be read
     */
    private static @Nullable Candidate scan(List<Path> paths, Collection<Class<? extends Annotation>> annotations) {
        var jar = paths.getFirst();
        try (var scan = new ClassGraph().overrideClasspath(paths).enableClassInfo().enableAnnotationInfo().ignoreClassVisibility().scan()) {
            var resources = scan.getResourcesWithPath(TineMetadata.FILE_NAME);
            if (resources.isEmpty()) {
                TridentLogger.warn("Skipping {}, it has no {}", jar.getFileName(), TineMetadata.FILE_NAME);
                return null;
            }

            var metadata = TineMetadata.parse(resources.getFirst().getContentAsString());
            var mainClasses = scan.getClassesWithAnnotation(Tine.class.getName()).getNames();
            if (mainClasses.size() > 1) {
                TridentLogger.error("Skipping Tine {} ({}), it has more than one @Tine class: {}", metadata.id(), jar.getFileName(), mainClasses);
                return null;
            }

            var classes = new HashMap<Class<? extends Annotation>, List<String>>();
            for (var annotation : annotations) {
                classes.put(annotation, scan.getClassesWithAnnotation(annotation.getName()).getNames());
            }

            return new Candidate(new TineContainer(metadata, paths, mainClasses.isEmpty() ? null : mainClasses.getFirst()), classes);
        } catch (IOException | RuntimeException e) {
            TridentLogger.error("Skipping " + jar.getFileName() + ", it could not be read", e);
            return null;
        }
    }

    /**
     * Removes candidates with unmet dependencies until all remaining dependencies are met, then orders the rest. Dependencies come
     * first, then lower {@link TineMetadata#order()}, then the ID. It skips candidates in a dependency cycle.
     *
     * @param loader the host that gives the loaded mods
     * @param candidates the candidates by Tine ID
     * @return the candidates in load order
     */
    private static List<Candidate> resolve(TridentHost loader, Map<String, Candidate> candidates) {
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
                var problem = Trident.findUnmetDependency(loader, metadata, remaining, versions);
                if (problem != null) {
                    TridentLogger.error("Skipping Tine {}, {}", metadata.id(), problem);
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
                    TridentLogger.error("Skipping Tine {}, it is part of or depends on a dependency cycle", id);
                }
            }
        }

        return ordered;
    }

    /**
     * Finds the first dependency of the Tine that is not installed or has a version outside its range. A loader mod
     * with no known version meets a dependency only if the range accepts any version.
     *
     * @param loader the host that tells which mods are loaded
     * @param metadata the metadata of the Tine
     * @param tines the remaining candidates by Tine ID
     * @param versions the versions of the loaded mods by mod ID
     * @return a description of the problem, or {@code null} if all dependencies are met
     */
    private static @Nullable String findUnmetDependency(TridentHost loader, TineMetadata metadata, Map<String, Candidate> tines, Map<String, String> versions) {
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

    /**
     * Holds the result of Tine discovery.
     *
     * @param classLoader the class loader that loads the Tine classes
     * @param classes the names of the Tine classes that have each annotation
     */
    public record Discovery(ClassLoader classLoader, Map<Class<? extends Annotation>, List<String>> classes) {}

    /**
     * Holds a Tine that was read but is not loaded yet.
     *
     * @param container the container of the Tine
     * @param classes the names of the Tine classes that have each annotation
     */
    private record Candidate(TineContainer container, Map<Class<? extends Annotation>, List<String>> classes) {}
}
