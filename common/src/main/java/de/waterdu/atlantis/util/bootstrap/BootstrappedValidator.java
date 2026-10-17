package de.waterdu.atlantis.util.bootstrap;

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

@SupportedAnnotationTypes({
        "de.waterdu.atlantis.util.bootstrap.Bootstrapped",
        "de.waterdu.atlantis.util.bootstrap.Entrypoint"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class BootstrappedValidator extends AbstractProcessor {

    public BootstrappedValidator() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = this.processingEnv.getMessager();
        for (var element : roundEnv.getElementsAnnotatedWith(Entrypoint.class)) {
            var method = (ExecutableElement) element;
            if (!method.getModifiers().contains(Modifier.STATIC) || !method.getParameters().isEmpty() || method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(Diagnostic.Kind.ERROR, "entrypoint must be a static ()V method", method);
            }
            if (method.getEnclosingElement().getAnnotation(Bootstrapped.class) == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "entrypoint must be declared in a bootstrapped class", method);
            }
        }
        for (var element : roundEnv.getElementsAnnotatedWith(Bootstrapped.class)) {
            var hasEntrypoint = false;
            for (var member : element.getEnclosedElements()) {
                if (member.getAnnotation(Entrypoint.class) != null) {
                    hasEntrypoint = true;
                    break;
                }
            }
            if (!hasEntrypoint) {
                messager.printMessage(Diagnostic.Kind.WARNING, "bootstrapped class has no @Entrypoint method", element);
            }
        }
        return true;
    }
}
