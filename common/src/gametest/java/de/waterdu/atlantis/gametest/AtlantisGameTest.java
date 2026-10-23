package de.waterdu.atlantis.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static void name(GameTestHelper)} method as a GameTest. Both loaders register these
 * through {@link AtlantisGameTests#collect()}, so test and template names are identical on NeoForge and Fabric.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtlantisGameTest {

    /** Structure template in the {@value AtlantisGameTests#NAMESPACE} namespace. */
    String template() default "empty";

    String batch() default "atlantis";

    int timeoutTicks() default 100;

    long setupTicks() default 0;

    boolean required() default true;
}
