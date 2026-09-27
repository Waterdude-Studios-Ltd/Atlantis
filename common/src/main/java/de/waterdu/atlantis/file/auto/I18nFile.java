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
 * Annotation for designating a file accessible via an {@link AtlantisConfig} instance created via
 * {@link AtlantisConfigProxy}, but only as an instance of {@link Configuration} as a i18n file.
 * <p>
 * This annotation is equivalent to the following annotation, where "this" refers to the annotated object:
 * {@literal @FileBackedObject(mod = Mod.MOD_ID, path = "i18n.json", i18n = true)}
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface I18nFile {

    /**
     * The {@link AtlantisConfig} this will be accessible from.
     *
     * @return The owner of this configuration's modid.
     */
    String mod();
}
