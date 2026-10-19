package de.waterdu.atlantis.file.auto;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({
        "de.waterdu.atlantis.file.auto.FileBackedObject",
        "de.waterdu.atlantis.file.auto.BasicConfiguration",
        "de.waterdu.atlantis.file.auto.BasicData",
        "de.waterdu.atlantis.file.auto.I18nFile"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class AtlantisConfigValidator extends AbstractProcessor {

    public AtlantisConfigValidator() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elements = this.processingEnv.getElementUtils();
        var types = this.processingEnv.getTypeUtils();
        var messager = this.processingEnv.getMessager();
        var interfaces = new ArrayList<TypeMirror>();
        interfaces.add(elements.getTypeElement("de.waterdu.atlantis.file.datatypes.Configuration").asType());
        var dataType = elements.getTypeElement("de.waterdu.atlantis.file.datatypes.Data").asType();
        interfaces.add(dataType);
        interfaces.add(elements.getTypeElement("de.waterdu.atlantis.file.datatypes.NamedData").asType());
        interfaces.add(elements.getTypeElement("de.waterdu.atlantis.file.datatypes.PlayerData").asType());
        for (var element : roundEnv.getElementsAnnotatedWith(FileBackedObject.class)) {
            this.testElement(element, elements, interfaces, types, dataType, messager, "FileBackedObject");
        }
        for (var element : roundEnv.getElementsAnnotatedWith(BasicConfiguration.class)) {
            this.testElement(element, elements, interfaces, types, dataType, messager, "BasicConfiguration");
        }
        for (var element : roundEnv.getElementsAnnotatedWith(BasicData.class)) {
            this.testElement(element, elements, interfaces, types, dataType, messager, "BasicData");
        }
        for (var element : roundEnv.getElementsAnnotatedWith(I18nFile.class)) {
            this.testElement(element, elements, interfaces, types, dataType, messager, "I18nFile");
        }
        return true;
    }

    private void testElement(Element element, Elements elements, List<TypeMirror> interfaces, Types types, TypeMirror dataType, Messager messager, String kind) {
        var type = element.asType();
        for (var tm : interfaces) {
            if (types.isAssignable(type, tm)) {
                if (types.isAssignable(type, dataType)) {
                    this.validateObjectMethods(elements, element, messager);
                }
                return;
            }
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "invalid " + kind + " implementation, must implement one of Configuration, Data, NamedData, PlayerData", element);
    }

    private boolean validateObjectMethods(Elements elements, Element element, Messager messager) {
        boolean hashCode = false;
        boolean equals = false;
        boolean toString = false;
        for (var member : elements.getAllMembers((TypeElement) element)) {
            if (member.getKind() == ElementKind.METHOD && !member.getModifiers().contains(Modifier.ABSTRACT) && !member.getEnclosingElement().getSimpleName().contentEquals("Object")) {
                if (member.getSimpleName().contentEquals("hashCode")) {
                    hashCode = true;
                } else if (member.getSimpleName().contentEquals("equals")) {
                    equals = true;
                } else if (member.getSimpleName().contentEquals("toString")) {
                    toString = true;
                }
            }
        }
        if (hashCode && equals && toString) {
            return true;
        } else {
            var missing = "";
            if (!hashCode) {
                missing += "hashCode";
            }
            if (!equals) {
                if (!missing.isEmpty()) {
                    missing += ", ";
                }
                missing += "equals";
            }
            if (!toString) {
                if (!missing.isEmpty()) {
                    missing += ", ";
                }
                missing += "toString";
            }
            messager.printMessage(Diagnostic.Kind.ERROR, "invalid " + element.getSimpleName() + " implementation, must override " + missing, element);
            return false;
        }
    }

    private boolean containsType(List<TypeMirror> types, TypeMirror type) {
        for (var other : types) {
            if (this.processingEnv.getTypeUtils().isSameType(type, other)) {
                return true;
            }
        }
        return false;
    }
}
