package de.waterdu.atlantis.util.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class whose {@link Entrypoint} methods the {@link Bootstrapper} calls at startup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bootstrapped {

    /**
     * @return the priority of the class in the call order.
     */
    Priority value() default Priority.NORMAL;

    enum Priority {

        FIRST,
        VERY_HIGH,
        HIGH,
        NORMAL,
        LOW,
        VERY_LOW,
        LAST,
    }
}
