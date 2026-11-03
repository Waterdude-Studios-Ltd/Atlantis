package de.waterdu.atlantis.util.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static, no-argument method of a {@link Bootstrapped} class that the {@link Bootstrapper} calls at startup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Entrypoint {}
