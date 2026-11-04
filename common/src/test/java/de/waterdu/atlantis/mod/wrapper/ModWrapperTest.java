package de.waterdu.atlantis.mod.wrapper;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ModWrapper}.
 */
class ModWrapperTest {

    private static final ModObjectKey KEY = new ModObjectKey("atlantis", "wrapped");

    /**
     * Checks that a wrapper of a {@link ModObject} dereferences the value and has a future that is complete.
     */
    @Test
    void objectWrapperHasCompletedFuture() {
        var wrapper = new Wrapper(new ModObject(KEY, "value"));

        assertThat(wrapper.dereference()).isEqualTo("value");
        assertThat(wrapper.future()).isCompletedWithValue("value");
    }

    /**
     * Checks that a wrapper of a complete {@link ModFuture} dereferences the value of the future.
     */
    @Test
    void completedFutureWrapperDereferencesValue() {
        var wrapper = new Wrapper(new ModFuture(KEY, CompletableFuture.completedFuture("value")));

        assertThat(wrapper.dereference()).isEqualTo("value");
        assertThat(wrapper.future()).isCompletedWithValue("value");
    }

    /**
     * Checks that a wrapper of a pending {@link ModFuture} dereferences {@code null} and keeps the same future.
     */
    @Test
    void pendingFutureWrapperDereferencesNull() {
        var future = new CompletableFuture<String>();
        var wrapper = new Wrapper(new ModFuture(KEY, future));

        assertThat(wrapper.dereference()).isNull();
        assertThat(wrapper.future()).isSameAs(future).isNotDone();
    }

    /**
     * A plain {@link ModWrapper} of a string.
     */
    private static final class Wrapper extends ModWrapper<String> {

        /**
         * Creates a wrapper of an object.
         *
         * @param object the object to wrap
         */
        Wrapper(ModObject object) {
            super(object);
        }

        /**
         * Creates a wrapper of a future.
         *
         * @param future the future to wrap
         */
        Wrapper(ModFuture future) {
            super(future);
        }
    }
}
