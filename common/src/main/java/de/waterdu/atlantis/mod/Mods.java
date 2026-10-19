package de.waterdu.atlantis.mod;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.common.event.meta.RegisterModExtensionsEventPayload;
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
import java.util.Map;
import java.util.function.Consumer;

@Bootstrapped(order = -100)
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
            .mainClass("de.waterdu.atlantis.Atlantis")
            .proxy(ModExtension.Loader.ALL, () -> new ModProxy() {})
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
            .mainClass("com.cobblemon.mod.common.CobblemonKt")
            .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, "de.waterdu.atlantis.mod.impl.cobblemon.CobblemonProxy")
            .delegates(PokemonDelegate.class)
            .language(ModExtension.Language.KOTLIN)
            .priority(ModExtension.Priority.HIGH)
            .build();

    public static final ModExtension IMPACTOR = ModExtension.builder()
            .modId("impactor")
            .mainClass("net.impactdev.impactor.api.Impactor")
            .reflectiveConstructorProxy(ModExtension.Loader.NEOFORGE_AND_FABRIC, "de.waterdu.atlantis.mod.impl.impactor.ImpactorProxy")
            .delegates(EconomyDelegate.class)
            .language(ModExtension.Language.JAVA)
            .priority(ModExtension.Priority.NORMAL)
            .build();

    private Mods() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

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

    private static boolean isActive(ModExtension extension) {
        if (!extension.supports(ACTIVE_LOADER) || !extension.mainClassPresent()) {
            return false;
        }
        return extension.modLoader() || Mods.modLoader().isModLoaded(extension.modId());
    }

    public static void registerExtensions(ModExtension... extensions) {
        for (var extension : extensions) {
            registerExtension(extension);
        }
    }

    public static void registerExtension(ModExtension extension) {
        EXTENSIONS.get(extension.priority()).put(extension.modId(), extension);
    }

    public static boolean isPresent(String modId) {
        return ACTIVE.containsKey(modId);
    }

    public static boolean isPresent(Class<? extends ModProxy> delegate) {
        return ACTIVE_BY_DELEGATE.containsKey(delegate);
    }

    public static ModExtension active(String modId) {
        return ACTIVE.getOrDefault(modId, NONE);
    }

    public static void forEach(Consumer<ModExtension> forEach) {
        for (var extension : ACTIVE.values()) {
            forEach.accept(extension);
        }
    }

    public static void forEachProxy(Consumer<ModProxy> forEach) {
        forEach(extension -> forEach.accept(extension.proxy(ACTIVE_LOADER)));
    }

    public static <P extends ModProxy> P proxy(String modId) {
        return active(modId).proxy(ACTIVE_LOADER);
    }

    public static <P extends ModProxy> P proxy(Class<P> delegate) {
        var extension = ACTIVE_BY_DELEGATE.get(delegate);
        if (extension == null) {
            throw new IllegalStateException("Delegate proxy extension not found");
        }
        return extension.proxy(ACTIVE_LOADER);
    }

    public static <P extends ModProxy> void proxyScope(String modId, Consumer<P> scope) {
        scope.accept(active(modId).proxy(ACTIVE_LOADER));
    }

    public static <P extends ModProxy> void proxyScope(Class<P> delegate, Consumer<P> scope) {
        scope.accept(proxy(delegate));
    }

    @SuppressWarnings("unchecked")
    public static <P extends ModLoaderDelegate> P modLoader() {
        return (P) proxy(ModLoaderDelegate.class);
    }

    @SuppressWarnings("unchecked")
    public static <P extends ModLoaderDelegate> void modLoaderScope(Consumer<P> scope) {
        proxyScope(ModLoaderDelegate.class, (Consumer<ModLoaderDelegate>) scope);
    }

    @SuppressWarnings("unchecked")
    public static <P extends PokemonDelegate> P pokemon() {
        return (P) proxy(PokemonDelegate.class);
    }

    @SuppressWarnings("unchecked")
    public static <P extends PokemonDelegate> void pokemonScope(Consumer<P> scope) {
        proxyScope(PokemonDelegate.class, (Consumer<PokemonDelegate>) scope);
    }

    @SuppressWarnings("unchecked")
    public static <P extends EconomyDelegate<?>> P economy() {
        return (P) proxy(EconomyDelegate.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <P extends EconomyDelegate<?>> void economyScope(Consumer<P> scope) {
        proxyScope(EconomyDelegate.class, (Consumer) scope);
    }

    public static ModEvents events() {
        return ModEvents.INSTANCE;
    }
}
