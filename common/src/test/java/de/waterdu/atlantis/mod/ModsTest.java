package de.waterdu.atlantis.mod;

import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.TestModLoader;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Drives the real {@code Mods.bootstrap()} entrypoint through the Bootstrapper, with the TestModLoader installed as
 * the mod loader.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class ModsTest {

    static final List<String> SETUPS = new ArrayList<>();

    @BeforeEach
    void clearSetups() {
        SETUPS.clear();
    }

    @Test
    void commonTestsRunAsFabric() {
        assertThat(classExists("net.neoforged.neoforge.common.NeoForge")).isFalse();
    }

    @Test
    void activatesLoadedExtensionAndRunsItsSetup(TestModLoader loader) {
        extension("fake_mod", ModExtension.Priority.NORMAL, recording("fake_mod")).build();
        loader.loadMod("fake_mod");

        bootstrap(loader);

        assertThat(Mods.isPresent("fake_mod")).isTrue();
        assertThat(Mods.active("fake_mod").modId()).isEqualTo("fake_mod");
        assertThat(SETUPS).containsExactly("fake_mod");
    }

    @Test
    void skipsExtensionWithoutProxyForActiveLoader(TestModLoader loader) {
        ModExtension.builder()
                .modId("neoforge_only")
                .mainClass(ModsTest.class)
                .proxy(ModExtension.Loader.NEOFORGE, recording("neoforge_only"))
                .language(ModExtension.Language.JAVA)
                .build();
        loader.loadMod("neoforge_only");

        bootstrap(loader);

        assertThat(Mods.isPresent("neoforge_only")).isFalse();
        assertThat(SETUPS).isEmpty();
    }

    @Test
    void skipsExtensionWhoseMainClassIsMissing(TestModLoader loader) {
        ModExtension.builder()
                .modId("absent_mod")
                .mainClass("de.waterdu.atlantis.DoesNotExist")
                .proxy(ModExtension.Loader.ALL, recording("absent_mod"))
                .language(ModExtension.Language.JAVA)
                .build();
        loader.loadMod("absent_mod");

        bootstrap(loader);

        assertThat(Mods.isPresent("absent_mod")).isFalse();
        assertThat(SETUPS).isEmpty();
    }

    @Test
    void skipsExtensionThatIsNotLoaded(TestModLoader loader) {
        extension("not_loaded", ModExtension.Priority.NORMAL, recording("not_loaded")).build();

        bootstrap(loader);

        assertThat(Mods.isPresent("not_loaded")).isFalse();
        assertThat(SETUPS).isEmpty();
    }

    @Test
    void activatesAtlantisItself(TestModLoader loader) {
        bootstrap(loader);

        assertThat(Mods.isPresent("atlantis")).isTrue();
    }

    @Test
    void setsUpExtensionsInPriorityOrder(TestModLoader loader) {
        extension("lowest", ModExtension.Priority.LOWEST, recording("lowest")).build();
        extension("high", ModExtension.Priority.HIGH, recording("high")).build();
        extension("normal", ModExtension.Priority.NORMAL, recording("normal")).build();
        extension("low", ModExtension.Priority.LOW, recording("low")).build();
        loader.loadMod("lowest").loadMod("high").loadMod("normal").loadMod("low");

        bootstrap(loader);

        assertThat(SETUPS).containsExactly("high", "normal", "low", "lowest");
    }

    @Test
    void higherPriorityExtensionWinsADuplicateDelegate(TestModLoader loader) {
        var preferred = mock(PokemonDelegate.class);
        var other = mock(PokemonDelegate.class);
        extension("preferred", ModExtension.Priority.HIGH, () -> preferred).delegates(PokemonDelegate.class).build();
        extension("other", ModExtension.Priority.NORMAL, () -> other).delegates(PokemonDelegate.class).build();
        loader.loadMod("preferred").loadMod("other");

        bootstrap(loader);

        assertThat(Mods.<PokemonDelegate>pokemon()).isSameAs(preferred);
        assertThat(Mods.isPresent("other")).isTrue();
        assertThat(Mods.isPresent(PokemonDelegate.class)).isTrue();
    }

    @Test
    void registerModExtensionsFiresAfterTheHighestTier(TestModLoader loader) {
        extension("highest", ModExtension.Priority.HIGHEST, recording("highest")).build();
        extension("high", ModExtension.Priority.HIGH, recording("high")).build();
        loader.loadMod("highest").loadMod("high");
        Mods.modLoader().event(UEvent.REGISTER_MOD_EXTENSIONS).listen(payload -> {
            SETUPS.add("event");
            return InteractionResult.PASS;
        });

        bootstrap(loader);

        assertThat(SETUPS).containsExactly("highest", "event", "high");
    }

    @Test
    void extensionRegisteredDuringTheEventIsActivated(TestModLoader loader) {
        loader.loadMod("late");
        Mods.modLoader().event(UEvent.REGISTER_MOD_EXTENSIONS).listen(payload -> {
            payload.register(extension("late", ModExtension.Priority.NORMAL, recording("late")).build());
            return InteractionResult.PASS;
        });

        bootstrap(loader);

        assertThat(Mods.isPresent("late")).isTrue();
        assertThat(SETUPS).containsExactly("late");
    }

    @Test
    void looksUpProxiesByModIdAndDelegate(TestModLoader loader) {
        var economy = mock(EconomyDelegate.class);
        extension("economy_mod", ModExtension.Priority.NORMAL, () -> economy).delegates(EconomyDelegate.class).build();
        loader.loadMod("economy_mod");

        bootstrap(loader);

        assertThat(Mods.<EconomyDelegate<?>>economy()).isSameAs(economy);
        assertThat(Mods.<ModProxy>proxy("economy_mod")).isSameAs(economy);
        assertThat((Object) Mods.proxy(EconomyDelegate.class)).isSameAs(economy);
        assertThat(Mods.<TestModLoader>modLoader()).isSameAs(loader);
        var scoped = new ArrayList<Object>();
        Mods.economyScope(scoped::add);
        Mods.proxyScope("economy_mod", scoped::add);
        assertThat(scoped).containsExactly(economy, economy);
    }

    @Test
    void missingDelegateThrowsAndMissingModFallsBackToNone(TestModLoader loader) {
        bootstrap(loader);

        assertThat(Mods.isPresent(PokemonDelegate.class)).isFalse();
        assertThatIllegalStateException().isThrownBy(Mods::pokemon);
        assertThat(Mods.active("missing")).isSameAs(Mods.NONE);
    }

    @Test
    void forEachVisitsActiveExtensionsAndTheirProxies(TestModLoader loader) {
        extension("visited", ModExtension.Priority.NORMAL, recording("visited")).build();
        loader.loadMod("visited");
        bootstrap(loader);
        var ids = new ArrayList<String>();
        var proxies = new ArrayList<ModProxy>();

        Mods.forEach(extension -> ids.add(extension.modId()));
        Mods.forEachProxy(proxies::add);

        assertThat(ids).contains("visited", "atlantis", "atlantis_test_loader");
        assertThat(proxies).hasSameSizeAs(ids).contains(Mods.<ModProxy>proxy("visited"));
    }

    @Test
    void reflectiveConstructorProxyOfAbsentModIsNeverLinked(TestModLoader loader) {
        assertThatNoException().isThrownBy(() -> ModExtension.builder()
                .modId("absent_integration")
                .mainClass("com.example.absent.Main")
                .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, "com.example.absent.Proxy")
                .language(ModExtension.Language.JAVA)
                .build());
        loader.loadMod("absent_integration");

        assertThatNoException().isThrownBy(() -> bootstrap(loader));
        assertThat(Mods.isPresent("absent_integration")).isFalse();
    }

    @Test
    void reflectiveConstructorProxyOfPresentModIsCreated(TestModLoader loader) {
        ModExtension.builder()
                .modId("reflective")
                .mainClass(ModsTest.class)
                .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, ReflectiveProxy.class.getName())
                .language(ModExtension.Language.JAVA)
                .build();
        loader.loadMod("reflective");

        bootstrap(loader);

        assertThat(Mods.<ModProxy>proxy("reflective")).isInstanceOf(ReflectiveProxy.class);
        assertThat(SETUPS).containsExactly("reflective");
    }

    @Test
    void resetForgetsExtensionsRegisteredByTests(TestModLoader loader) {
        extension("forgotten", ModExtension.Priority.NORMAL, recording("forgotten")).build();
        Mods.resetForTesting();
        Bootstrapper.resetForTesting();
        Mods.activateForTesting(ModExtension.builder()
                .modId("atlantis_test_loader")
                .mainClass(ModsTest.class)
                .proxy(ModExtension.Loader.ALL, () -> loader)
                .delegates(de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate.class)
                .modLoader()
                .language(ModExtension.Language.JAVA)
                .priority(ModExtension.Priority.HIGHEST)
                .build());
        loader.loadMod("forgotten");

        bootstrap(loader);

        assertThat(Mods.isPresent("forgotten")).isFalse();
        assertThat(Mods.isPresent("atlantis")).isTrue();
    }

    private static void bootstrap(TestModLoader loader) {
        loader.annotated(Bootstrapped.class, Mods.class);
        Bootstrapper.bootstrap(loader);
    }

    private static ModExtension.Builder extension(String modId, ModExtension.Priority priority, Supplier<? extends ModProxy> proxy) {
        return ModExtension.builder()
                .modId(modId)
                .mainClass(ModsTest.class)
                .proxy(ModExtension.Loader.ALL, proxy)
                .language(ModExtension.Language.JAVA)
                .priority(priority);
    }

    private static Supplier<ModProxy> recording(String name) {
        return () -> new ModProxy() {
            @Override
            public void setup() {
                SETUPS.add(name);
            }
        };
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ModsTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static final class ReflectiveProxy implements ModProxy {

        public ReflectiveProxy() {}

        @Override
        public void setup() {
            SETUPS.add("reflective");
        }
    }
}
