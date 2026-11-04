package de.waterdu.atlantis.mod;

import de.waterdu.atlantis.mod.plugin.PluginBridge;
import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Describes an optional mod, loader or plugin that Atlantis integrates with, and holds its proxy for each loader.
 * Create one with {@link #builder()}.
 */
@NullMarked
public final class ModExtension {

    private final String modId;
    private final Lazy.Throwing<Class<?>> mainClass;
    private final Map<Loader, Lazy<? extends ModProxy>> proxies;
    private final Set<Loader> supportedLoaders;
    private final List<Class<? extends ModProxy>> delegateProxies;
    private final boolean modLoader;
    private final boolean plugin;
    private final Language language;
    private final Priority priority;

    /**
     * Creates an extension. The main class loads only when {@link #mainClass()} is first called.
     *
     * @param modId the mod ID
     * @param mainClass the binary name of the main class of the mod
     * @param proxies the proxy of each loader
     * @param supportedLoaders the loaders that have a proxy
     * @param delegateProxies the delegate interfaces that the proxy implements
     * @param modLoader {@code true} if the extension is a mod loader
     * @param plugin {@code true} if the extension is a server plugin
     * @param language the language of the mod
     * @param priority the activation priority
     */
    private ModExtension(
            String modId,
            String mainClass,
            Map<Loader, Lazy<? extends ModProxy>> proxies,
            Set<Loader> supportedLoaders,
            List<Class<? extends ModProxy>> delegateProxies,
            boolean modLoader,
            boolean plugin,
            Language language,
            Priority priority
    ) {
        this.modId = modId;
        this.mainClass = Lazy.throwingOf(() -> Class.forName(mainClass, false, ModExtension.class.getClassLoader()));
        this.proxies = proxies;
        this.supportedLoaders = supportedLoaders;
        this.delegateProxies = delegateProxies;
        this.modLoader = modLoader;
        this.plugin = plugin;
        this.language = language;
        this.priority = priority;
    }

    /**
     * @return the mod ID of the extension
     */
    public String modId() {
        return this.modId;
    }

    /**
     * Gets the main class of the mod. The class loads on the first call, without initialization.
     *
     * @return the main class
     * @throws Throwable if the class cannot be loaded
     */
    public Class<?> mainClass() throws Throwable {
        return this.mainClass.getThrowing();
    }

    /**
     * Checks if the main class of the mod can be loaded.
     *
     * @return {@code true} if {@link #mainClass()} does not throw
     */
    public boolean mainClassPresent() {
        try {
            this.mainClass();
            return true;
        } catch (Throwable __) {
            return false;
        }
    }

    /**
     * Creates the proxy on the first call. If the extension has no proxy for the loader, returns a {@link ModProxy} that
     * does nothing.
     */
    @SuppressWarnings("unchecked")
    public <P extends ModProxy> P proxy(Loader loader) {
        return (P) this.proxies.get(loader).get();
    }

    /**
     * Checks if the extension has a proxy for the loader.
     *
     * @param loader the loader to check
     * @return {@code true} if a proxy was added for the loader
     */
    public boolean supports(Loader loader) {
        return this.supportedLoaders.contains(loader);
    }

    /**
     * @return the delegate interfaces that the proxy implements
     */
    public List<Class<? extends ModProxy>> delegateProxies() {
        return this.delegateProxies;
    }

    /**
     * @return {@code true} if the extension is a mod loader
     */
    public boolean modLoader() {
        return this.modLoader;
    }

    /**
     * @return {@code true} if the extension is a server plugin
     */
    public boolean plugin() {
        return this.plugin;
    }

    /**
     * Checks if the extension is a plugin and the plugin bridge can reach it.
     *
     * @return {@code true} if the extension is a plugin and {@link PluginBridge#canMake} is {@code true}. If the
     *     plugin classes cannot be linked, this is {@code false}.
     */
    public boolean isPluginAndLoaded() {
        if (!this.plugin) {
            return false;
        }
        try {
            return PluginBridge.canMake(this.modId);
        } catch (LinkageError __) {
            return false;
        }
    }

    /**
     * @return the language that the mod is written in
     */
    public Language language() {
        return this.language;
    }

    /**
     * @return the activation priority of the extension
     */
    public Priority priority() {
        return this.priority;
    }

    /**
     * Identifies a loader that an extension can have a proxy for. {@link #ALL}, {@link #NEOFORGE_AND_FABRIC} and
     * {@link #SPIGOT} also add the proxy for other loaders in {@link Builder#proxy}.
     */
    public enum Loader {

        NEOFORGE,
        FABRIC,
        NEOFORGE_AND_FABRIC,
        SPIGOT,
        ALL,
        OTHER,
        ;

        /**
         * Checks if this value is {@link #ALL} or {@link #OTHER}. {@link #ALL} skips these values when it adds a proxy.
         *
         * @return {@code true} for {@link #ALL} and {@link #OTHER}
         */
        public boolean special() {
            return this == ALL || this == OTHER;
        }
    }

    /**
     * Identifies the language that a mod is written in.
     */
    public enum Language {

        JAVA,
        KOTLIN,
        OTHER,
    }

    /**
     * Sets the order in which extensions activate, from {@link #HIGHEST} to {@link #LOWEST}.
     */
    public enum Priority {

        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST,
    }

    /**
     * Creates a new builder.
     *
     * @return a builder with no values set
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link ModExtension}. The mod ID, main class, language and at least one proxy must be set.
     */
    @NullUnmarked
    public static class Builder {

        private String modId;
        private String mainClass;
        private final Map<Loader, Lazy<? extends ModProxy>> proxies = new HashMap<>() {{
            for (var loader : Loader.values()) {
                this.put(loader, Lazy.of(() -> new ModProxy() {}));
            }
        }};
        private boolean addedProxy = false;
        private final Set<Loader> supportedLoaders = EnumSet.noneOf(Loader.class);
        private final List<Class<? extends ModProxy>> delegateProxies = new ArrayList<>();
        private boolean modLoader = false;
        private boolean plugin = false;
        private Language language;
        private Priority priority = Priority.NORMAL;

        /**
         * Sets the mod ID.
         *
         * @param modId the mod ID
         * @return this builder
         */
        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        /**
         * Sets the main class by name, so that the class does not load now.
         *
         * @param mainClass the binary name of the main class of the mod
         * @return this builder
         */
        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        /**
         * Sets the main class to the name of the class.
         *
         * @param mainClass the main class of the mod
         * @return this builder
         */
        public Builder mainClass(Class<?> mainClass) {
            this.mainClass = mainClass.getName();
            return this;
        }

        /**
         * {@link Loader#ALL} and {@link Loader#NEOFORGE_AND_FABRIC} add the proxy for each loader they include.
         * {@link Loader#SPIGOT} also adds it for NeoForge and Fabric, and sets the main class to {@code org.bukkit.Bukkit}
         * if none is set.
         *
         * @param proxy called once, when the proxy is first used
         */
        public Builder proxy(Loader loader, Supplier<? extends ModProxy> proxy) {
            this.addedProxy = true;
            switch (loader) {
                case ALL -> {
                    for (var it : Loader.values()) {
                        if (!it.special()) {
                            this.proxy(it, proxy);
                        }
                    }
                }
                case NEOFORGE_AND_FABRIC -> {
                    this.proxy(Loader.NEOFORGE, proxy);
                    this.proxy(Loader.FABRIC, proxy);
                }
                case SPIGOT -> {
                    if (this.supportedLoaders.isEmpty() && this.mainClass == null) {
                        this.mainClass("org.bukkit.Bukkit");
                    }
                    this.proxy(Loader.NEOFORGE_AND_FABRIC, proxy);
                    this.proxies.put(loader, Lazy.of(proxy));
                    this.supportedLoaders.add(loader);
                }
                default -> {
                    this.proxies.put(loader, Lazy.of(proxy));
                    this.supportedLoaders.add(loader);
                }
            }
            return this;
        }

        /**
         * Use this for proxy classes that link against an optional mod, so that the class loads only when the proxy is
         * first used.
         *
         * @param classpath the binary name of a proxy class with a public no-argument constructor
         */
        public Builder reflectiveConstructorProxy(Loader loader, String classpath) {
            Supplier<? extends ModProxy> proxy = () -> {
                try {
                    return (ModProxy) Class.forName(classpath).getConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to create proxy via reflective constructor " + classpath, e);
                }
            };

            return this.proxy(loader, proxy);
        }

        /**
         * Gets the proxy from a singleton when the proxy is first used.
         *
         * @param classpath the binary name of the class that holds the singleton
         * @param proxyGetterName the name of a public, no-argument instance method that returns the proxy
         * @param instanceGetterName the name of a static, no-argument method declared in the class that returns the
         *     singleton
         */
        public Builder reflectiveMethodProxy(
                Loader loader,
                String classpath,
                String proxyGetterName,
                String instanceGetterName
        ) {
            Supplier<? extends ModProxy> proxy = () -> {
                try {
                    var clazz = Class.forName(classpath);
                    var instance = clazz.getDeclaredMethod(instanceGetterName).invoke(null);
                    return (ModProxy) clazz.getMethod(proxyGetterName).invoke(instance);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to create proxy via reflective method " + classpath + "#" + proxyGetterName, e);
                }
            };

            return this.proxy(loader, proxy);
        }

        /**
         * @param delegateProxy the delegate interfaces that the proxy implements. {@code Mods} finds the active
         *     extension by these, for example in {@link Mods#pokemon()}.
         */
        @SafeVarargs
        public final Builder delegates(Class<? extends ModProxy>... delegateProxy) {
            this.delegateProxies.addAll(List.of(delegateProxy));
            return this;
        }

        /**
         * Marks the extension as a mod loader. A mod loader extension is active without a mod list check, and
         * {@code Mods} does not call its {@link ModProxy#setup()}.
         */
        public Builder modLoader() {
            this.modLoader = true;
            return this;
        }

        /**
         * Marks the extension as a server plugin, for hybrid servers. A plugin extension is also active when the plugin
         * is loaded but the mod loader does not know of it.
         */
        public Builder plugin() {
            this.plugin = true;
            return this;
        }

        /**
         * Sets the language that the mod is written in.
         *
         * @param language the language
         * @return this builder
         */
        public Builder language(Language language) {
            this.language = language;
            return this;
        }

        /**
         * Extensions activate from {@link Priority#HIGHEST} to {@link Priority#LOWEST}. When two active extensions give
         * the same delegate, the first one to activate wins.
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Also registers the extension with {@link Mods#registerExtension}. Throws {@link IllegalStateException} if the
         * mod ID, main class, language or a proxy is not set.
         */
        public ModExtension build() {
            if (this.modId == null ||
                    this.mainClass == null ||
                    !this.addedProxy ||
                    this.language == null ||
                    this.priority == null
            ) {
                throw new IllegalStateException("Cannot build ModExtension from partial arguments");
            }

            var extension = new ModExtension(
                    this.modId,
                    this.mainClass,
                    this.proxies,
                    this.supportedLoaders,
                    this.delegateProxies,
                    this.modLoader,
                    this.plugin,
                    this.language,
                    this.priority
            );
            Mods.registerExtension(extension);
            return extension;
        }
    }
}
