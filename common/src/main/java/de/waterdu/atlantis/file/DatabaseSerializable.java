package de.waterdu.atlantis.file;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DatabaseSerializable {

    String type() default DatabaseEncoders.Defaults.JSON;

    String quoteMark() default DatabaseEncoders.QuoteMarks.SINGLE;

    String encoder() default DatabaseEncoders.Defaults.JSON;

    boolean primaryKey() default false;

    boolean autoIncrement() default false;

    boolean uniqueKey() default false;

    boolean notNull() default true;
}
