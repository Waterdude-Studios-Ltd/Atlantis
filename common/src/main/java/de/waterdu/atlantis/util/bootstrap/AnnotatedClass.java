package de.waterdu.atlantis.util.bootstrap;

import java.lang.annotation.Annotation;

/**
 * Holds a class that the {@link Bootstrapper} scan found, together with the annotation it found on it.
 *
 * @param clazz the annotated class
 * @param annotation the annotation instance on the class
 */
public record AnnotatedClass(Class<?> clazz, Annotation annotation) {}
