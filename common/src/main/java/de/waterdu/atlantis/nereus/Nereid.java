package de.waterdu.atlantis.nereus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An interface marking the main class of a Nereid, an Atlantis-loaded mod via the {@link NereusLoader}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Nereid {

    /**
     * The mod ID of this Nereid. Analogous to NeoForge/Fabirc mod IDs.
     * @return The mod ID of this Nereid.
     */
    String value();

    /**
     * The order in which to load Nereus extensions. Lower = earlier.
     * @return The load order value of this mod.
     */
    int order() default 0;

    /**
     * Marks the initialization method to be called. This happens before any modloader-specific lifecycle events and
     * calls.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface Harbor {}
}
