package de.waterdu.atlantis.trident;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the main class of a Tine. A Tine is a mod that the {@link Trident} loads. A Tine jar can contain at most one
 * main class. A {@code tine.json} file in the jar's resources declares the Tine's ID, version, and dependencies.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Tine {

    /**
     * Marks the static, parameterless initialization method of a Tine. Trident calls barbs in Tine load order after
     * {@link de.waterdu.atlantis.mod.Mods} has bootstrapped. It calls them before all other bootstrap entrypoints and
     * modloader-specific lifecycle events.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface Barb {}
}
