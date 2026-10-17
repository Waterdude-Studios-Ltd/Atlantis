package de.waterdu.atlantis.util.bootstrap;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import io.github.classgraph.ClassGraph;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

    public static void forEachAnnotatedClass(Class<? extends Annotation> annotation, Consumer<AnnotatedClass> forEach) {
        ANNOTATED_CLASSES.getOrDefault(annotation, NO_ANNOTATED_CLASSES).forEach(forEach);
    }

    public static void bootstrap() {
        if (BOOTSTRAPPED.getAndSet(true)) {
            return;
        }

        try (var scan = new ClassGraph().enableAllInfo().enableExternalClasses().scan()) {
            for (var annotation : ANNOTATIONS) {
                for (var info : scan.getClassesWithAnnotation(annotation)) {
                    ANNOTATED_CLASSES
                            .computeIfAbsent(annotation, k -> new ArrayList<>())
                            .add(new AnnotatedClass(info.loadClass(), info.getAnnotationInfo(annotation)));
                }
            }
        }

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, annotated -> {
            var clazz = annotated.clazz();
            for (var method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Entrypoint.class)) {
                    continue;
                }

                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    AtlantisLogger.warn("Skipping entrypoint {}#{}, entrypoints must be static and take no parameters", clazz.getName(), method.getName());
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(null);
                } catch (InvocationTargetException e) {
                    AtlantisLogger.error("Failed to bootstrap class " + clazz.getName(), e.getCause());
                    continue;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    AtlantisLogger.error("Failed to bootstrap class " + clazz.getName(), e);
                    continue;
                }

                AtlantisLogger.debug("Bootstrapped class {} via {}", clazz.getName(), method.getName());
            }
        });
    }
}
