package de.waterdu.atlantis.util.bootstrap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("de.waterdu.atlantis.util.bootstrap.Bootstrapped")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class BootstrappedValidator extends AbstractProcessor {

    public BootstrappedValidator() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elements = this.processingEnv.getElementUtils();
        var messager = this.processingEnv.getMessager();
        for (var element : roundEnv.getElementsAnnotatedWith(Bootstrapped.class)) {
            var entrypoint = element.getAnnotation(Bootstrapped.class).entrypoint();
            var hasBootstrapMethod = false;
            for (var member : elements.getAllMembers((TypeElement) element)) {
                if (member.getKind() == ElementKind.METHOD) {
                    if (member.getSimpleName().contentEquals(entrypoint)) {
                        hasBootstrapMethod = true;
                        break;
                    }
                }
            }
            if (!hasBootstrapMethod) {
                messager.printMessage(Diagnostic.Kind.ERROR, "bootstrapped class requires static " + entrypoint + "()V method", element);
            }
        }
        return true;
    }
}
