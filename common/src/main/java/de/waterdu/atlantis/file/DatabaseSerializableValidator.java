package de.waterdu.atlantis.file;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({
        "de.waterdu.atlantis.file.DatabaseSerializable",
        "de.waterdu.atlantis.file.AllowedTypes"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DatabaseSerializableValidator extends AbstractProcessor {

    public DatabaseSerializableValidator() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = this.processingEnv.getMessager();
        var types = this.processingEnv.getTypeUtils();
        outer:
        for (var element : roundEnv.getElementsAnnotatedWith(DatabaseSerializable.class)) {
            var annotation = element.getAnnotation(DatabaseSerializable.class);
            var type = annotation.type();
            var erasedType = types.erasure(element.asType());
            var javaType = erasedType.toString();
            var allowedTypes = this.getAllowedTypes(type);
            if (allowedTypes != null) {
                try {
                    if (!allowedTypes.isEmpty()) {
                        for (var clazz : allowedTypes) {
                            if (types.isSameType(types.erasure(clazz), erasedType)) {
                                continue outer;
                            }
                        }
                        messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported SQL type " + type + " for JVM type " + javaType, element);
                    }
                } catch (NullPointerException ignored) {}
            }
        }
        return true;
    }

    private List<TypeMirror> getAllowedTypes(String type) {
        var defaults = this.processingEnv.getElementUtils().getTypeElement("de.waterdu.atlantis.file.DatabaseEncoders.Defaults");
        if (defaults == null) {
            return null;
        }
        for (var field : defaults.getEnclosedElements()) {
            if (field instanceof VariableElement variable && variable.getConstantValue() instanceof String value && value.equalsIgnoreCase(type)) {
                return this.getClassArrayValueFromAnnotation(field, AllowedTypes.class, "value");
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<TypeMirror> getClassArrayValueFromAnnotation(Element element, Class<? extends Annotation> annotation, String paramName) {
        var elements = this.processingEnv.getElementUtils();
        var types = this.processingEnv.getTypeUtils();

        var values = new ArrayList<TypeMirror>();

        for (var am : element.getAnnotationMirrors()) {
            if (types.isSameType(am.getAnnotationType(), elements.getTypeElement(annotation.getCanonicalName()).asType())) {
                for (var entry : am.getElementValues().entrySet()) {
                    if (paramName.equals(entry.getKey().getSimpleName().toString())) {
                        var classesTypes = (List<AnnotationValue>) entry.getValue().getValue();
                        for (var next : classesTypes) {
                            values.add((TypeMirror) next.getValue());
                        }
                    }
                }
            }
        }
        return values;
    }
}
