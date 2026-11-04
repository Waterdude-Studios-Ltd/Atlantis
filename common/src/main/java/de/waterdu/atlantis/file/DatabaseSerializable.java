package de.waterdu.atlantis.file;

import java.lang.annotation.*;

/**
 * Marks a field of a configuration as a column for database storage.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DatabaseSerializable {

    /**
     * The SQL column type. If it is one of {@link DatabaseEncoders.Defaults}, {@link DatabaseSerializableValidator}
     * checks it against the field type at compile time.
     */
    String type() default DatabaseEncoders.Defaults.JSON;

    /**
     * The mark around the encoded value in SQL queries. It must be one of {@link DatabaseEncoders.QuoteMarks}. The
     * value is escaped for the mark. With {@link DatabaseEncoders.QuoteMarks#NONE}, the value must be a number,
     * {@code NULL}, {@code TRUE} or {@code FALSE}, or the write fails.
     */
    String quoteMark() default DatabaseEncoders.QuoteMarks.SINGLE;

    /**
     * The name of an encoder in {@link DatabaseEncoders}. Case does not matter.
     * If no encoder has this name, the config does not register.
     */
    String encoder() default DatabaseEncoders.Defaults.JSON;

    /**
     * @return {@code true} if the column is the primary key of the table
     */
    boolean primaryKey() default false;

    /**
     * Atlantis does not write this column. The database sets the value.
     */
    boolean autoIncrement() default false;

    /**
     * @return {@code true} if the column has a unique key
     */
    boolean uniqueKey() default false;

    /**
     * @return {@code true} if the column cannot hold {@code NULL}
     */
    boolean notNull() default true;
}
