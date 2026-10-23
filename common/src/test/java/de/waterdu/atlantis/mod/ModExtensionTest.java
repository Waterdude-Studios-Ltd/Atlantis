package de.waterdu.atlantis.mod;

import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(AtlantisTestEnvironment.class)
class ModExtensionTest {

    private static ModExtension.Builder complete() {
        return ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .proxy(ModExtension.Loader.FABRIC, () -> new ModProxy() {})
                .language(ModExtension.Language.JAVA);
    }

    @Test
    void buildsWithDefaults() {
        var extension = complete().build();

        assertThat(extension.modId()).isEqualTo("example");
        assertThat(extension.priority()).isEqualTo(ModExtension.Priority.NORMAL);
        assertThat(extension.language()).isEqualTo(ModExtension.Language.JAVA);
        assertThat(extension.modLoader()).isFalse();
        assertThat(extension.delegateProxies()).isEmpty();
    }

    @Test
    void rejectsPartialBuilders() {
        assertThatIllegalStateException().isThrownBy(() -> complete().modId(null).build());
        assertThatIllegalStateException().isThrownBy(() -> complete().mainClass((String) null).build());
        assertThatIllegalStateException().isThrownBy(() -> complete().language(null).build());
        assertThatIllegalStateException().isThrownBy(() -> complete().priority(null).build());
        assertThatIllegalStateException().isThrownBy(() -> ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .language(ModExtension.Language.JAVA)
                .build());
    }

    @Test
    void recordsDelegatesAndFlags() {
        var extension = complete()
                .delegates(PokemonDelegate.class, EconomyDelegate.class)
                .modLoader()
                .language(ModExtension.Language.KOTLIN)
                .priority(ModExtension.Priority.HIGH)
                .build();

        assertThat(extension.delegateProxies()).containsExactly(PokemonDelegate.class, EconomyDelegate.class);
        assertThat(extension.modLoader()).isTrue();
        assertThat(extension.language()).isEqualTo(ModExtension.Language.KOTLIN);
        assertThat(extension.priority()).isEqualTo(ModExtension.Priority.HIGH);
    }

    @Test
    void singleLoaderProxy() {
        var extension = complete().build();

        assertThat(extension.supports(ModExtension.Loader.FABRIC)).isTrue();
        for (var loader : ModExtension.Loader.values()) {
            if (loader != ModExtension.Loader.FABRIC) {
                assertThat(extension.supports(loader)).as(loader.name()).isFalse();
            }
        }
    }

    @Test
    void neoForgeAndFabricProxy() {
        var proxy = new ModProxy() {};
        var extension = ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .proxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, () -> proxy)
                .language(ModExtension.Language.JAVA)
                .build();

        assertThat(extension.supports(ModExtension.Loader.NEOFORGE)).isTrue();
        assertThat(extension.supports(ModExtension.Loader.FABRIC)).isTrue();
        assertThat(extension.supports(ModExtension.Loader.SPIGOT)).isFalse();
        assertThat(extension.<ModProxy>proxy(ModExtension.Loader.NEOFORGE)).isSameAs(proxy);
        assertThat(extension.<ModProxy>proxy(ModExtension.Loader.FABRIC)).isSameAs(proxy);
    }

    @Test
    void allProxyCoversEveryLoaderItCanRunOn() {
        var extension = ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .proxy(ModExtension.Loader.ALL, () -> new ModProxy() {})
                .language(ModExtension.Language.JAVA)
                .build();

        assertThat(extension.supports(ModExtension.Loader.NEOFORGE)).isTrue();
        assertThat(extension.supports(ModExtension.Loader.FABRIC)).isTrue();
        assertThat(extension.supports(ModExtension.Loader.SPIGOT)).isTrue();
        assertThat(extension.supports(ModExtension.Loader.NEOFORGE_AND_FABRIC)).isFalse();
        assertThat(extension.supports(ModExtension.Loader.ALL)).isFalse();
        assertThat(extension.supports(ModExtension.Loader.OTHER)).isFalse();
    }

    @Test
    void unsupportedLoaderGetsNoOpProxy() {
        var extension = complete().build();

        assertThat(extension.<ModProxy>proxy(ModExtension.Loader.NEOFORGE)).isNotNull();
    }

    @Test
    void proxyIsCreatedLazilyAndOnce() {
        var created = new AtomicInteger();
        var extension = ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .proxy(ModExtension.Loader.FABRIC, () -> {
                    created.incrementAndGet();
                    return new ModProxy() {};
                })
                .language(ModExtension.Language.JAVA)
                .build();

        assertThat(created).hasValue(0);
        ModProxy first = extension.proxy(ModExtension.Loader.FABRIC);
        ModProxy second = extension.proxy(ModExtension.Loader.FABRIC);

        assertThat(second).isSameAs(first);
        assertThat(created).hasValue(1);
    }

    @Test
    void reflectiveConstructorProxy() {
        var extension = ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .reflectiveConstructorProxy(ModExtension.Loader.FABRIC, ReflectiveProxy.class.getName())
                .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE, "de.waterdu.atlantis.DoesNotExist")
                .language(ModExtension.Language.JAVA)
                .build();

        assertThat(extension.<ModProxy>proxy(ModExtension.Loader.FABRIC)).isInstanceOf(ReflectiveProxy.class);
        assertThatThrownBy(() -> extension.proxy(ModExtension.Loader.NEOFORGE)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("de.waterdu.atlantis.DoesNotExist");
    }

    @Test
    void reflectiveMethodProxy() {
        var extension = ModExtension.builder()
                .modId("example")
                .mainClass(ModExtensionTest.class)
                .reflectiveMethodProxy(ModExtension.Loader.FABRIC, ProxyHolder.class.getName(), "proxy", "instance")
                .language(ModExtension.Language.JAVA)
                .build();

        assertThat(extension.<ModProxy>proxy(ModExtension.Loader.FABRIC)).isSameAs(ProxyHolder.INSTANCE.proxy());
    }

    @Test
    void mainClassPresenceDoesNotInitialiseTheClass() {
        var present = complete().mainClass("de.waterdu.atlantis.mod.ModExtensionTest$NotInitialised").build();
        var absent = complete().mainClass("de.waterdu.atlantis.DoesNotExist").build();

        assertThat(present.mainClassPresent()).isTrue();
        assertThat(NotInitialisedProbe.INITIALISED).isFalse();
        assertThat(absent.mainClassPresent()).isFalse();
        assertThatThrownBy(absent::mainClass).isInstanceOf(ClassNotFoundException.class);
    }

    public static final class ReflectiveProxy implements ModProxy {

        public ReflectiveProxy() {}
    }

    public static final class ProxyHolder {

        static final ProxyHolder INSTANCE = new ProxyHolder();

        private final ModProxy proxy = new ModProxy() {};

        public static ProxyHolder instance() {
            return INSTANCE;
        }

        public ModProxy proxy() {
            return this.proxy;
        }
    }

    static final class NotInitialisedProbe {

        static boolean INITIALISED = false;
    }

    @SuppressWarnings("unused")
    static final class NotInitialised {

        static {
            NotInitialisedProbe.INITIALISED = true;
        }
    }
}
