package de.waterdu.atlantis.util.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.trident.TridentLoader;
import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Bootstrapper {

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);
    private static final List<AnnotatedClass> NO_ANNOTATED_CLASSES = List.of();

    private static final List<Class<? extends Annotation>> ANNOTATIONS = List.of(
            Bootstrapped.class,
            BasicConfiguration.class,
            BasicData.class,
            I18nFile.class,
            FileBackedObject.class
    );
    private static final Map<Class<? extends Annotation>, List<AnnotatedClass>> ANNOTATED_CLASSES = new HashMap<>();

    private Bootstrapper() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    @VisibleForTesting
    public static void resetForTesting() {
        ANNOTATED_CLASSES.clear();
        BOOTSTRAPPED.set(false);
        TridentLoader.resetForTesting();
    }

    public static void forEachAnnotatedClass(Class<? extends Annotation> annotation, Consumer<AnnotatedClass> forEach) {
        ANNOTATED_CLASSES.getOrDefault(annotation, NO_ANNOTATED_CLASSES).forEach(forEach);
    }

    public static void bootstrap(ModLoaderDelegate loader) {
        if (BOOTSTRAPPED.getAndSet(true)) {
            return;
        }

        var discovery = TridentLoader.discover(loader, ANNOTATIONS);
        var classLoader = discovery.classLoader();
        var scanned = new HashMap<>(loader.scanAnnotatedClasses(ANNOTATIONS));

        discovery.classes().forEach((annotation, names) -> scanned.merge(annotation, names, (first, second) -> {
            var merged = new ArrayList<>(first);
            merged.addAll(second);
            return merged;
        }));

        for (var entry : scanned.entrySet()) {
            var annotation = entry.getKey();
            for (var className : entry.getValue()) {
                Class<?> clazz;
                try {
                    clazz = Class.forName(className, false, classLoader);
                } catch (ClassNotFoundException | LinkageError e) {
                    AtlantisLogger.debug("Skipping annotated class {}: {}", className, e.toString());
                    continue;
                }
                var instance = clazz.getAnnotation(annotation);
                if (instance != null) {
                    ANNOTATED_CLASSES.computeIfAbsent(annotation, k -> new ArrayList<>()).add(new AnnotatedClass(clazz, instance));
                }
            }
        }
        var bootstrapped = ANNOTATED_CLASSES.get(Bootstrapped.class);
        if (bootstrapped != null) {
            bootstrapped.sort(Comparator.comparingInt(annotated -> ((Bootstrapped) annotated.annotation()).order()));
        }

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, annotated -> Bootstrapper.invokeStatic(annotated.clazz(), Entrypoint.class));
    }

    public static void invokeStatic(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (var method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(annotation)) {
                continue;
            }

            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                AtlantisLogger.warn("Skipping @{} method {}#{}, it must be static and take no parameters", annotation.getSimpleName(), clazz.getName(), method.getName());
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(null);
            } catch (InvocationTargetException e) {
                AtlantisLogger.error("Failed to invoke @" + annotation.getSimpleName() + " method " + clazz.getName() + "#" + method.getName(), e.getCause());
                continue;
            } catch (ReflectiveOperationException | RuntimeException e) {
                AtlantisLogger.error("Failed to invoke @" + annotation.getSimpleName() + " method " + clazz.getName() + "#" + method.getName(), e);
                continue;
            }

            AtlantisLogger.debug("Invoked @{} method {}#{}", annotation.getSimpleName(), clazz.getName(), method.getName());
        }
    }
}
