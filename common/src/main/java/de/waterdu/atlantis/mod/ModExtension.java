package de.waterdu.atlantis.mod;

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

@NullMarked
public final class ModExtension {

    private final String modId;
    private final Lazy.Throwing<Class<?>> mainClass;
    private final Map<Loader, Lazy<? extends ModProxy>> proxies;
    private final Set<Loader> supportedLoaders;
    private final List<Class<? extends ModProxy>> delegateProxies;
    private final boolean modLoader;
    private final Language language;
    private final Priority priority;

    private ModExtension(
            String modId,
            String mainClass,
            Map<Loader, Lazy<? extends ModProxy>> proxies,
            Set<Loader> supportedLoaders,
            List<Class<? extends ModProxy>> delegateProxies,
            boolean modLoader,
            Language language,
            Priority priority
    ) {
        this.modId = modId;
        this.mainClass = Lazy.throwingOf(() -> Class.forName(mainClass, false, ModExtension.class.getClassLoader()));
        this.proxies = proxies;
        this.supportedLoaders = supportedLoaders;
        this.delegateProxies = delegateProxies;
        this.modLoader = modLoader;
        this.language = language;
        this.priority = priority;
    }

    public String modId() {
        return this.modId;
    }

    public Class<?> mainClass() throws Throwable {
        return this.mainClass.getThrowing();
    }

    public boolean mainClassPresent() {
        try {
            this.mainClass();
            return true;
        } catch (Throwable __) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends ModProxy> P proxy(Loader loader) {
        return (P) this.proxies.get(loader).get();
    }

    public boolean supports(Loader loader) {
        return this.supportedLoaders.contains(loader);
    }

    public List<Class<? extends ModProxy>> delegateProxies() {
        return this.delegateProxies;
    }

    public boolean modLoader() {
        return this.modLoader;
    }

    public Language language() {
        return this.language;
    }

    public Priority priority() {
        return this.priority;
    }

    public enum Loader {

        NEOFORGE,
        FABRIC,
        NEOFORGE_AND_FABRIC,
        SPIGOT,
        ALL,
        OTHER,
        ;

        public boolean special() {
            return this == ALL || this == OTHER;
        }
    }

    public enum Language {

        JAVA,
        KOTLIN,
        OTHER,
    }

    public enum Priority {

        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST,
    }

    public static Builder builder() {
        return new Builder();
    }

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
        private Language language;
        private Priority priority = Priority.NORMAL;

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public Builder mainClass(Class<?> mainClass) {
            this.mainClass = mainClass.getName();
            return this;
        }

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
                default -> {
                    this.proxies.put(loader, Lazy.of(proxy));
                    this.supportedLoaders.add(loader);
                }
            }
            return this;
        }

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

        @SafeVarargs
        public final Builder delegates(Class<? extends ModProxy>... delegateProxy) {
            this.delegateProxies.addAll(List.of(delegateProxy));
            return this;
        }

        public Builder modLoader() {
            this.modLoader = true;
            return this;
        }

        public Builder language(Language language) {
            this.language = language;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

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
                    this.language,
                    this.priority
            );
            Mods.registerExtension(extension);
            return extension;
        }
    }
}
