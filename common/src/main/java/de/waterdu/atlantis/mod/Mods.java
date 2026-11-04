package de.waterdu.atlantis.mod;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.common.event.meta.RegisterModExtensionsEventPayload;
import de.waterdu.atlantis.mod.impl.impactor.ImpactorProxy;
import de.waterdu.atlantis.mod.impl.vault.VaultProxy;
import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.proxy.delegate.EconomyDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.ModLoaderDelegate;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers the mod extensions, activates the ones that are present, and gives access to their proxies and delegates.
 * Bootstraps at priority FIRST, before the other entrypoints.
 */
@Bootstrapped(Bootstrapped.Priority.FIRST)
public final class Mods {

    private static final Map<ModExtension.Priority, Map<String, ModExtension>> EXTENSIONS = new EnumMap<>(ModExtension.Priority.class) {{
        for (var priority : ModExtension.Priority.values()) {
            this.put(priority, new HashMap<>());
        }
    }};

    private static final ModExtension.Loader ACTIVE_LOADER;
    static {
        ModExtension.Loader loader;
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge", false, Mods.class.getClassLoader());
            loader = ModExtension.Loader.NEOFORGE;
        } catch (ClassNotFoundException e) {
            loader = ModExtension.Loader.FABRIC;
        }
        ACTIVE_LOADER = loader;
    }

    private static final Map<String, ModExtension> ACTIVE = new HashMap<>();
    private static final Map<Class<? extends ModProxy>, ModExtension> ACTIVE_BY_DELEGATE = new HashMap<>();

    public static final String NEOFORGE_ID = "neoforge";
    public static final String FABRIC_ID = "fabric";

    public static final ModExtension NONE = ModExtension.builder()
            .modId(Atlantis.ID)
            .proxy(ModExtension.Loader.ALL, () -> new ModProxy() {})
            .mainClass("de.waterdu.atlantis.Atlantis")
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.LOWEST)
            .build();

    public static final ModExtension NEOFORGE = ModExtension.builder()
            .modId(NEOFORGE_ID)
            .mainClass("net.neoforged.neoforge.common.NeoForge")
            .reflectiveMethodProxy(
                    ModExtension.Loader.NEOFORGE,
                    "de.waterdu.atlantis.AtlantisNeoForge",
                    "proxy",
                    "instance"
            )
            .delegates(ModLoaderDelegate.class)
            .modLoader()
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.HIGHEST)
            .build();

    public static final ModExtension FABRIC = ModExtension.builder()
            .modId(FABRIC_ID)
            .mainClass("net.fabricmc.loader.api.FabricLoader")
            .reflectiveMethodProxy(
                    ModExtension.Loader.FABRIC,
                    "de.waterdu.atlantis.AtlantisFabric",
                    "proxy",
                    "instance"
            )
            .delegates(ModLoaderDelegate.class)
            .modLoader()
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.HIGHEST)
            .build();

    public static final ModExtension PIXELMON = ModExtension.builder()
            .modId("pixelmon")
            .mainClass("com.pixelmonmod.pixelmon.Pixelmon")
            .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE, "de.waterdu.atlantis.mod.impl.pixelmon.PixelmonProxy")
            .delegates(PokemonDelegate.class, EconomyDelegate.class)
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.HIGH)
            .build();

    public static final ModExtension COBBLEMON = ModExtension.builder()
            .modId("cobblemon")
            .mainClass("com.cobblemon.mod.common.Cobblemon")
            .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, "de.waterdu.atlantis.mod.impl.cobblemon.CobblemonProxy")
            .delegates(PokemonDelegate.class)
            .language(ModExtension.Language.KOTLIN)
            .priority(ModExtension.Priority.HIGH)
            .build();

    public static final ModExtension IMPACTOR = ModExtension.builder()
            .modId("impactor")
            .mainClass("net.impactdev.impactor.api.Impactor")
            .proxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, ImpactorProxy::new)
            .delegates(EconomyDelegate.class)
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.LOW)
            .build();

    public static final ModExtension VAULT = ModExtension.builder()
            .modId("vault")
            .proxy(ModExtension.Loader.SPIGOT, VaultProxy::new)
            .delegates(EconomyDelegate.class)
            .plugin()
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.LOW)
            .build();

    private static final List<ModExtension> BUILT_IN = List.of(NONE, NEOFORGE, FABRIC, PIXELMON, COBBLEMON, IMPACTOR, VAULT);

    private Mods() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Activates the extensions without the checks of {@link #bootstrap()}. An existing delegate is not replaced.
     *
     * @param extensions the extensions to activate
     */
    @VisibleForTesting
    public static void activateForTesting(ModExtension... extensions) {
        for (var extension : extensions) {
            ACTIVE.put(extension.modId(), extension);
            for (var delegate : extension.delegateProxies()) {
                ACTIVE_BY_DELEGATE.putIfAbsent(delegate, extension);
            }
        }
    }

    /**
     * Deactivates all extensions, removes all registered extensions, and registers the built-in extensions again.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        ACTIVE.clear();
        ACTIVE_BY_DELEGATE.clear();
        EXTENSIONS.values().forEach(Map::clear);
        BUILT_IN.forEach(Mods::registerExtension);
    }

    /**
     * Activates each present extension, from the highest priority to the lowest, and calls {@link ModProxy#setup()} on
     * the ones that are not mod loaders. After the {@link ModExtension.Priority#HIGHEST} extensions activate, emits
     * REGISTER_MOD_EXTENSIONS so that listeners can add extensions. When two extensions give the same delegate, the
     * first one keeps it.
     */
    @Entrypoint
    @SuppressWarnings("unused")
    private static void bootstrap() {

        for (var priority : ModExtension.Priority.values()) {
            for (var extension : EXTENSIONS.get(priority).values()) {
                if (Mods.isActive(extension)) {
                    ACTIVE.put(extension.modId(), extension);
                    for (var delegate : extension.delegateProxies()) {
                        if (ACTIVE_BY_DELEGATE.containsKey(delegate)) {
                            AtlantisLogger.warn("Duplicate delegate proxy {} used by mod {}, ignoring", delegate.getName(), extension.modId());
                        } else {
                            ACTIVE_BY_DELEGATE.put(delegate, extension);
                        }
                    }
                    if (!extension.modLoader()) {
                        extension.proxy(ACTIVE_LOADER).setup();
                    }
                }
            }
            if (priority == ModExtension.Priority.HIGHEST) {
                Mods.modLoader().event(UEvent.REGISTER_MOD_EXTENSIONS).emit(new RegisterModExtensionsEventPayload()).applyExtensions(EXTENSIONS);
            }
        }
    }

    /**
     * Checks if the extension must activate. It must support the active loader and its main class must exist. It must
     * also be a mod loader, a loaded mod or a loaded plugin.
     *
     * @param extension the extension to check
     * @return {@code true} if the extension must activate
     */
    private static boolean isActive(ModExtension extension) {
        if (!extension.supports(ACTIVE_LOADER) || !extension.mainClassPresent()) {
            return false;
        }
        return extension.modLoader() || Mods.modLoader().isModLoaded(extension.modId()) || extension.isPluginAndLoaded();
    }

    /**
     * Registration has no effect after {@code Mods} bootstraps. {@link ModExtension.Builder#build()} already registers the
     * extension.
     */
    public static void registerExtensions(ModExtension... extensions) {
        for (var extension : extensions) {
            registerExtension(extension);
        }
    }

    /**
     * Registration has no effect after {@code Mods} bootstraps. {@link ModExtension.Builder#build()} already registers the
     * extension.
     */
    public static void registerExtension(ModExtension extension) {
        EXTENSIONS.get(extension.priority()).put(extension.modId(), extension);
    }

    /**
     * Checks for an active extension, not for the mod itself. Use {@link ModLoaderDelegate#isModLoaded} for any mod.
     */
    public static boolean isPresent(String modId) {
        return ACTIVE.containsKey(modId);
    }

    /**
     * Checks if an active extension gives the delegate.
     *
     * @param delegate the delegate interface
     * @return {@code true} if an active extension gives the delegate
     */
    public static boolean isDelegatePresent(Class<? extends ModProxy> delegate) {
        return ACTIVE_BY_DELEGATE.containsKey(delegate);
    }

    /**
     * Applies the mapper to the proxy that gives the delegate. If no active extension gives it, gets the value from
     * {@code ifNotPresent}.
     *
     * @param delegate the delegate interface
     * @param ifNotPresent gives the value when the delegate is not present
     * @param mapper maps the proxy to the value
     * @param <T> the type of the value
     * @param <D> the type of the delegate
     * @return the mapped value, or the value from {@code ifNotPresent}
     */
    public static <T, D extends ModProxy> T mapDelegate(Class<D> delegate, Supplier<T> ifNotPresent, Function<D, T> mapper) {
        var extension = ACTIVE_BY_DELEGATE.get(delegate);
        if (extension == null) {
            return ifNotPresent.get();
        }
        return mapper.apply(extension.proxy(ACTIVE_LOADER));
    }

    /**
     * @return the mod's active extension, or {@link #NONE} if it has none
     */
    public static ModExtension active(String modId) {
        return ACTIVE.getOrDefault(modId, NONE);
    }

    /**
     * Runs the action for each active extension.
     *
     * @param forEach the action to run
     */
    public static void forEach(Consumer<ModExtension> forEach) {
        for (var extension : ACTIVE.values()) {
            forEach.accept(extension);
        }
    }

    /**
     * Runs the action for the proxy of each active extension, for the active loader.
     *
     * @param forEach the action to run
     */
    public static void forEachProxy(Consumer<ModProxy> forEach) {
        forEach(extension -> forEach.accept(extension.proxy(ACTIVE_LOADER)));
    }

    /**
     * @return the proxy of the mod's active extension. If the mod has no active extension, this is a {@link ModProxy}
     *     that does nothing, and the cast to {@code P} fails.
     */
    public static <P extends ModProxy> P proxy(String modId) {
        return active(modId).proxy(ACTIVE_LOADER);
    }

    /**
     * Throws {@link IllegalStateException} if no active extension provides the delegate. Use
     * {@link #isDelegatePresent} to check first.
     */
    public static <P extends ModProxy> P proxy(Class<P> delegate) {
        var extension = ACTIVE_BY_DELEGATE.get(delegate);
        if (extension == null) {
            throw new IllegalStateException("Delegate proxy extension not found");
        }
        return extension.proxy(ACTIVE_LOADER);
    }

    /**
     * Runs the scope with the proxy of the mod's active extension. If the mod has no active extension, the proxy is a
     * {@link ModProxy} that does nothing.
     *
     * @param modId the mod ID
     * @param scope receives the proxy
     * @param <P> the type of the proxy
     */
    public static <P extends ModProxy> void proxyScope(String modId, Consumer<P> scope) {
        scope.accept(active(modId).proxy(ACTIVE_LOADER));
    }

    /**
     * Runs the scope with the proxy that gives the delegate.
     *
     * @param delegate the delegate interface
     * @param scope receives the proxy
     * @param <P> the type of the delegate
     * @throws IllegalStateException if no active extension gives the delegate
     */
    public static <P extends ModProxy> void proxyScope(Class<P> delegate, Consumer<P> scope) {
        scope.accept(proxy(delegate));
    }

    /**
     * Gets the delegate of the active mod loader.
     *
     * @param <P> the type of the delegate
     * @return the mod loader delegate
     */
    @SuppressWarnings("unchecked")
    public static <P extends ModLoaderDelegate> P modLoader() {
        return (P) proxy(ModLoaderDelegate.class);
    }

    /**
     * Runs the scope with the delegate of the active mod loader.
     *
     * @param scope receives the mod loader delegate
     * @param <P> the type of the delegate
     */
    @SuppressWarnings("unchecked")
    public static <P extends ModLoaderDelegate> void modLoaderScope(Consumer<P> scope) {
        proxyScope(ModLoaderDelegate.class, (Consumer<ModLoaderDelegate>) scope);
    }

    /**
     * Throws {@link IllegalStateException} if no supported Pokémon mod is present.
     */
    @SuppressWarnings("unchecked")
    public static <P extends PokemonDelegate> P pokemon() {
        return (P) proxy(PokemonDelegate.class);
    }

    /**
     * Runs the scope with the delegate of the active Pokémon mod.
     *
     * @param scope receives the Pokémon delegate
     * @param <P> the type of the delegate
     * @throws IllegalStateException if no supported Pokémon mod is present
     */
    @SuppressWarnings("unchecked")
    public static <P extends PokemonDelegate> void pokemonScope(Consumer<P> scope) {
        proxyScope(PokemonDelegate.class, (Consumer<PokemonDelegate>) scope);
    }

    /**
     * Throws {@link IllegalStateException} if no supported economy mod or plugin is present.
     */
    @SuppressWarnings("unchecked")
    public static <P extends EconomyDelegate<?>> P economy() {
        return (P) proxy(EconomyDelegate.class);
    }

    /**
     * Runs the scope with the delegate of the active economy mod or plugin.
     *
     * @param scope receives the economy delegate
     * @param <P> the type of the delegate
     * @throws IllegalStateException if no supported economy mod or plugin is present
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <P extends EconomyDelegate<?>> void economyScope(Consumer<P> scope) {
        proxyScope(EconomyDelegate.class, (Consumer) scope);
    }

    /**
     * @return the emitter of the loader-neutral events that mixins fire
     */
    public static ModEvents events() {
        return ModEvents.INSTANCE;
    }
}
