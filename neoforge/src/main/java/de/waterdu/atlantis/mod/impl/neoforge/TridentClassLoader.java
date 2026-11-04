package de.waterdu.atlantis.mod.impl.neoforge;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.Launcher;
import de.waterdu.atlantis.trident.Trident;
import de.waterdu.atlantis.trident.TridentLogger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads Tine classes on NeoForge. It is a child of the class loader of Atlantis.
 */
public final class TridentClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates the loader. Use {@link #install}.
     *
     * @param urls the URLs of the Tine jars and directories
     * @param parent the parent class loader
     */
    private TridentClassLoader(URL[] urls, ClassLoader parent) {
        super("trident", urls, parent);
    }

    /**
     * Also sets the new loader as the fallback of {@code parent}, so that other mods can resolve Tine classes.
     * This only occurs if {@code parent} is a {@link ModuleClassLoader}.
     */
    public static ClassLoader install(List<Path> jars, ClassLoader parent) {
        var urls = Trident.jarsToURLs(jars);
        var loader = new TridentClassLoader(urls, parent);
        if (parent instanceof ModuleClassLoader moduleClassLoader) {
            moduleClassLoader.setFallbackClassLoader(new Fallback(loader, Launcher.class.getClassLoader()));
        } else {
            TridentLogger.warn("Atlantis was not loaded by a module classloader, other mods won't be able to resolve Tine classes");
        }
        return loader;
    }

    /**
     * Finds a class in the Tine jars only, without asking the parent.
     *
     * @param name the binary name of the class
     * @return the loaded class
     * @throws ClassNotFoundException if no Tine jar has the class
     */
    private Class<?> findOwnClass(String name) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            var loaded = this.findLoadedClass(name);
            return loaded != null ? loaded : this.findClass(name);
        }
    }

    /**
     * Serves as the fallback of a {@link ModuleClassLoader}. It looks in the Tine jars first, then asks the previous
     * fallback.
     */
    private static final class Fallback extends ClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }

        private final TridentClassLoader trident;
        private final ClassLoader previous;

        /**
         * Creates the fallback.
         *
         * @param trident the loader of the Tine classes
         * @param previous the loader to ask when the Tine jars do not have a class or resource
         */
        private Fallback(TridentClassLoader trident, ClassLoader previous) {
            super("trident-fallback", null);
            this.trident = trident;
            this.previous = previous;
        }

        /**
         * Loads the class from the Tine jars, or from the previous loader if they do not have it.
         *
         * @param name the binary name of the class
         * @param resolve not used
         * @return the loaded class
         * @throws ClassNotFoundException if the previous loader also does not find the class
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return this.trident.findOwnClass(name);
            } catch (ClassNotFoundException e) {
                return this.previous.loadClass(name);
            }
        }

        /**
         * Finds the resource in the Tine jars, or in the previous loader if they do not have it.
         *
         * @param name the name of the resource
         * @return the URL of the resource, or {@code null} if no loader has it
         */
        @Override
        public URL getResource(String name) {
            var resource = this.trident.findResource(name);
            return resource != null ? resource : this.previous.getResource(name);
        }

        /**
         * Finds the resources in the Tine jars. Only if they have none, it asks the previous loader.
         *
         * @param name the name of the resources
         * @return the URLs of the resources
         * @throws IOException if an I/O error occurs
         */
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            var resources = this.trident.findResources(name);
            return resources.hasMoreElements() ? resources : this.previous.getResources(name);
        }
    }
}
