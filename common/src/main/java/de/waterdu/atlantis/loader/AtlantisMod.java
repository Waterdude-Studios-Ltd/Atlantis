package de.waterdu.atlantis.loader;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface AtlantisMod {

    /**
     * The mod ID of this Atlantis mod. Analogous to NeoForge/Fabirc mod IDs.
     * @return The mod ID of this mod.
     */
    String value();
}
