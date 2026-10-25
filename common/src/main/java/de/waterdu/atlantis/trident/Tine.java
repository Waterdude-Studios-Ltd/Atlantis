package de.waterdu.atlantis.trident;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the main class of a Tine, a mod loaded by the {@link TridentLoader}. A Tine jar may contain at most one.
 * The Tine's ID, version, and dependencies are declared in a {@code tine.json} at the root of its jar.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Tine {

    /**
     * Marks the static, parameterless initialization method of a Tine. Barbs are called in Tine load order once
     * {@link de.waterdu.atlantis.mod.Mods} has bootstrapped, before any other bootstrap entrypoints and
     * modloader-specific lifecycle events.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface Barb {}
}
