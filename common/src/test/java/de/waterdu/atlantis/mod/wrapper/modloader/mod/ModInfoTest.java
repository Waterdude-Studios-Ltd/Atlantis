package de.waterdu.atlantis.mod.wrapper.modloader.mod;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ModInfoTest {

    @Test
    void versionCheckIsLazy() {
        var calls = new AtomicInteger();
        var info = new ModInfo("atlantis", "Atlantis", "9.0.1", List.of("Waterdude"), null, () -> {
            calls.incrementAndGet();
            return new VersionCheck(VersionStatus.OUTDATED, "9.1.0");
        });

        assertThat(calls).hasValue(0);
        var check = info.versionCheck().get();

        assertThat(calls).hasValue(1);
        assertThat(check.status()).isEqualTo(VersionStatus.OUTDATED);
        assertThat(check.target()).isEqualTo("9.1.0");
        assertThat(info.issueTracker()).isNull();
    }

    @Test
    void unsupportedHasNoTarget() {
        assertThat(VersionCheck.UNSUPPORTED.status()).isEqualTo(VersionStatus.UNSUPPORTED);
        assertThat(VersionCheck.UNSUPPORTED.target()).isNull();
    }
}
