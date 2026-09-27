package de.waterdu.atlantis.file;

import java.lang.annotation.*;

/**
 * Comment to be inserted into an Atlantis Object Notation file on creation.
 * Comments will be updated automatically via AtlantisConfig where they are out of date.
 * An Atlantis Object Notation file must have a comment associated with the type.
 * Field comments are optional.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface AonComment {

    /**
     * Comments to apply to the annotated element.
     * Empty strings denote a blank line.
     *
     * @return The comments to apply to the annotated element.
     */
    String[] value();

    /**
     * How many # characters should be prefixed to a given comment line.
     * If a value is not present for a given {@link AonComment#value()} index, 1 will be used.
     *
     * @return How many # characters should be prefixed to a given comment line.
     */
    int[] indents() default 1;
}
