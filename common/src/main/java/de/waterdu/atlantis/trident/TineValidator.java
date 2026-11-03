package de.waterdu.atlantis.trident;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Checks {@link Tine} classes and {@link Tine.Barb} methods at compile time. A barb must be a static, no-argument
 * {@code void} method in a {@link Tine} class, and a {@link Tine} class must have at most one barb.
 */
@SupportedAnnotationTypes({
        "de.waterdu.atlantis.trident.Tine",
        "de.waterdu.atlantis.trident.Tine.Barb"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class TineValidator extends AbstractProcessor {

    /**
     * Creates the processor.
     */
    public TineValidator() {
        super();
    }

    /**
     * Reports an error for each barb that is not valid and for each {@link Tine} class with more than one barb. It
     * reports a warning for each {@link Tine} class with no barb.
     *
     * @param annotations the annotation types to process
     * @param roundEnv the environment of the current round
     * @return {@code true} always, so that no other processor claims the annotations
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = this.processingEnv.getMessager();

        for (var element : roundEnv.getElementsAnnotatedWith(Tine.Barb.class)) {
            var method = (ExecutableElement) element;
            if (!method.getModifiers().contains(Modifier.STATIC) || !method.getParameters().isEmpty() || method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(Diagnostic.Kind.ERROR, "barb must be a static ()V method", method);
            }

            if (method.getEnclosingElement().getAnnotation(Tine.class) == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "barb must be declared in a @Tine class", method);
            }
        }

        for (var element : roundEnv.getElementsAnnotatedWith(Tine.class)) {
            var barbs = 0;
            for (var member : element.getEnclosedElements()) {
                if (member.getAnnotation(Tine.Barb.class) != null) {
                    barbs++;
                }
            }

            if (barbs == 0) {
                messager.printMessage(Diagnostic.Kind.WARNING, "tine class has no @Tine.Barb method", element);
            } else if (barbs > 1) {
                messager.printMessage(Diagnostic.Kind.ERROR, "tine class has more than one @Tine.Barb method", element);
            }
        }

        return true;
    }
}
