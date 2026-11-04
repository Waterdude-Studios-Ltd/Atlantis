package de.waterdu.atlantis.file;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a column type field with the Java field types that it can store.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface AllowedTypes {

    /**
     * The Java field types that {@link DatabaseSerializableValidator} accepts for this column type at compile time.
     * An empty array accepts every type.
     */
    Class<?>[] value();
}
