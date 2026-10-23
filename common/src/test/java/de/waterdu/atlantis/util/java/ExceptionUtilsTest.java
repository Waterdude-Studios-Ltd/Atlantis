package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.catchThrowable;

class ExceptionUtilsTest {

    @Test
    void throwsIllegalState() {
        assertThatIllegalStateException()
                .isThrownBy(ExceptionUtils::throwCannotInstantiateClassException)
                .withMessage("Cannot instantiate this class");
    }

    @Test
    void utilityClassesCannotBeInstantiated() throws NoSuchMethodException {
        var constructor = NumberUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        var thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).isInstanceOf(InvocationTargetException.class);
        assertThat(thrown.getCause()).isInstanceOf(IllegalStateException.class);
    }
}
