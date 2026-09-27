package de.waterdu.atlantis.util.bootstrap;

import io.github.classgraph.AnnotationInfo;

public record AnnotatedClass(Class<?> clazz, AnnotationInfo info) {

    public void test() {
        this.info.getParameterValues()
    }
}
