package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.datatypes.Configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Designates a file that is accessible through an {@link AtlantisConfig} instance that
 * {@link AtlantisConfigProxy} creates. The file is only accessible as an instance of {@link Configuration}.
 * <p>
 * This annotation is equivalent to the annotation below. In it, "this" refers to the annotated object:
 * {@literal @FileBackedObject(mod = Mod.MOD_ID, path = this.class.getSimpleName().toLowerCase() + ".json")}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface BasicConfiguration {

    /**
     * The {@link AtlantisConfig} that gives access to this file.
     *
     * @return the mod ID of the owner of this configuration
     */
    String mod();
}
