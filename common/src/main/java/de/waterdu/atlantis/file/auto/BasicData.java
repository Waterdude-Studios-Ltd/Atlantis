package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.datatypes.Data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a file that is accessible through an {@link AtlantisConfig} instance created by
 * {@link AtlantisConfigProxy}. The file is accessible only as an instance of {@link Data}.
 * <p>
 * This annotation is equivalent to the following annotation, where "this" refers to the annotated object:
 * {@literal @FileBackedObject(mod = Mod.MOD_ID, path = this.class.getSimpleName().toLowerCase(), force = true)}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface BasicData {

    /**
     * The {@link AtlantisConfig} that this file is accessible from.
     *
     * @return the mod ID of the owner of this configuration
     */
    String mod();
}
