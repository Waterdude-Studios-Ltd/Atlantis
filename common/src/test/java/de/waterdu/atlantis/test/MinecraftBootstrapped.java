package de.waterdu.atlantis.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bootstraps vanilla Minecraft (built-in registries, components and codecs) once per JVM, before the annotated test
 * class runs. It does not apply mixins.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MinecraftBootstrapExtension.class)
public @interface MinecraftBootstrapped {
}
