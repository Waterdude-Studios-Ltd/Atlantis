package de.waterdu.atlantis.util.bootstrap;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.collection.Pair;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collection;
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

        Bootstrapper.forEachAnnotatedClass(Bootstrapped.class, clazz -> {

        });

        for (var bootstrapped : ANNOTATED_CLASSES.get(Bootstrapped.class)) {

        }

        // TODO: https://github.com/classgraph/classgraph replacement for this rather than relying on loader internals
        if (!BOOTSTRAPPED.getAndSet(true)) {
            final var annotations = ModList.get().getAllScanData().stream()
                    .map(ModFileScanData::getAnnotations)
                    .flatMap(Collection::stream)
                    .filter(data -> BOOTSTRAPPED_TYPE.equals(data.annotationType()))
                    .toList();

            for (var data : annotations) {
                var className = data.clazz().getClassName();
                try {
                    var clazz = Class.forName(className, false, Atlantis.class.getClassLoader());
                    var method = clazz.getDeclaredMethod(clazz.getAnnotation(Bootstrapped.class).entrypoint());
                    var accessFlags = method.accessFlags();
                    var inaccessible = accessFlags.contains(AccessFlag.PRIVATE) || accessFlags.contains(AccessFlag.PROTECTED);
                    if (inaccessible) {
                        method.setAccessible(true);
                    }
                    method.invoke(null);
                    if (inaccessible) {
                        method.setAccessible(false);
                    }
                } catch (Exception ignored) {
                    continue;
                }
                AtlantisLogger.debug("Bootstrapped class {}", className);
            }
        }
    }
}
