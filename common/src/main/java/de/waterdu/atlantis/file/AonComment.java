package de.waterdu.atlantis.file;

import java.lang.annotation.*;

/**
 * Comment that Atlantis inserts into an Atlantis Object Notation file when it creates the file.
 * AtlantisConfig automatically updates comments that are out of date.
 * An Atlantis Object Notation file must have a comment for the type.
 * Field comments are optional.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface AonComment {

    /**
     * Comments to apply to the annotated element.
     * An empty string gives a blank line.
     *
     * @return the comments to apply to the annotated element
     */
    String[] value();

    /**
     * The number of # characters to put before each comment line.
     * If there is no value for a {@link AonComment#value()} index, the number is 1.
     *
     * @return the number of # characters to put before each comment line
     */
    int[] indents() default 1;
}
