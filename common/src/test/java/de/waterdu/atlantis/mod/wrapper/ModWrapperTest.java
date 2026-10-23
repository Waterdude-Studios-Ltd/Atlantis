package de.waterdu.atlantis.mod.wrapper;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ModWrapperTest {

    private static final ModObjectKey KEY = new ModObjectKey("atlantis", "wrapped");

    @Test
    void objectWrapperHasCompletedFuture() {
        var wrapper = new Wrapper(new ModObject(KEY, "value"));

        assertThat(wrapper.dereference()).isEqualTo("value");
        assertThat(wrapper.future()).isCompletedWithValue("value");
    }

    @Test
    void completedFutureWrapperDereferencesValue() {
        var wrapper = new Wrapper(new ModFuture(KEY, CompletableFuture.completedFuture("value")));

        assertThat(wrapper.dereference()).isEqualTo("value");
        assertThat(wrapper.future()).isCompletedWithValue("value");
    }

    @Test
    void pendingFutureWrapperDereferencesNull() {
        var future = new CompletableFuture<String>();
        var wrapper = new Wrapper(new ModFuture(KEY, future));

        assertThat(wrapper.dereference()).isNull();
        assertThat(wrapper.future()).isSameAs(future).isNotDone();
    }

    private static final class Wrapper extends ModWrapper<String> {

        Wrapper(ModObject object) {
            super(object);
        }

        Wrapper(ModFuture future) {
            super(future);
        }
    }
}
