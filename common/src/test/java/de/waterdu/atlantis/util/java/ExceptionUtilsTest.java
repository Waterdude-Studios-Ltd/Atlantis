package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests {@link ExceptionUtils}.
 */
class ExceptionUtilsTest {

    /**
     * Checks that {@code throwCannotInstantiateClassException} throws an {@link IllegalStateException} with the
     * expected message.
     */
    @Test
    void throwsIllegalState() {
        assertThatIllegalStateException()
                .isThrownBy(ExceptionUtils::throwCannotInstantiateClassException)
                .withMessage("Cannot instantiate this class");
    }

    /**
     * Checks that the private constructor of {@link NumberUtils} throws an {@link IllegalStateException}.
     */
    @Test
    void utilityClassesCannotBeInstantiated() throws NoSuchMethodException {
        var constructor = NumberUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        var thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).isInstanceOf(InvocationTargetException.class);
        assertThat(thrown.getCause()).isInstanceOf(IllegalStateException.class);
    }
}
