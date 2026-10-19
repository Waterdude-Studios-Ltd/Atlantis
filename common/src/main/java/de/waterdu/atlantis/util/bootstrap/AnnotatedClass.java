package de.waterdu.atlantis.util.bootstrap;

import java.lang.annotation.Annotation;

public record AnnotatedClass(Class<?> clazz, Annotation annotation) {}
