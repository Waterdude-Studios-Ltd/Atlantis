package de.waterdu.atlantis.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code public static void name(GameTestHelper)} method as a GameTest. Both loaders register these methods
 * through {@link AtlantisGameTests#collect()}. Thus, test and template names are identical on NeoForge and Fabric.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtlantisGameTest {

    /**
     * Structure template in the {@value AtlantisGameTests#NAMESPACE} namespace.
     */
    String template() default "empty";

    /**
     * @return the batch that the test runs in
     */
    String batch() default "atlantis";

    /**
     * @return the number of ticks after which the test fails if it did not succeed
     */
    int timeoutTicks() default 100;

    /**
     * @return the number of ticks to wait after the structure is placed before the test starts
     */
    long setupTicks() default 0;

    /**
     * @return {@code true} if a failure of this test fails the test run
     */
    boolean required() default true;
}
