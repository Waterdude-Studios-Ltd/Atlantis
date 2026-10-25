package de.waterdu.atlantis.mod.impl.neoforge;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.Launcher;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.trident.TridentLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

public final class TridentClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private TridentClassLoader(URL[] urls, ClassLoader parent) {
        super("trident", urls, parent);
    }

    public static ClassLoader install(List<Path> jars, ClassLoader parent) {
        var urls = TridentLoader.jarsToURLs(jars);
        var loader = new TridentClassLoader(urls, parent);
        if (parent instanceof ModuleClassLoader moduleClassLoader) {
            moduleClassLoader.setFallbackClassLoader(new Fallback(loader, Launcher.class.getClassLoader()));
        } else {
            AtlantisLogger.warn("Atlantis was not loaded by a module classloader, other mods won't be able to resolve Tine classes");
        }
        return loader;
    }

    private Class<?> findOwnClass(String name) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            var loaded = this.findLoadedClass(name);
            return loaded != null ? loaded : this.findClass(name);
        }
    }

    private static final class Fallback extends ClassLoader {

        static {
            ClassLoader.registerAsParallelCapable();
        }

        private final TridentClassLoader trident;
        private final ClassLoader previous;

        private Fallback(TridentClassLoader trident, ClassLoader previous) {
            super("trident-fallback", null);
            this.trident = trident;
            this.previous = previous;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return this.trident.findOwnClass(name);
            } catch (ClassNotFoundException e) {
                return this.previous.loadClass(name);
            }
        }

        @Override
        public URL getResource(String name) {
            var resource = this.trident.findResource(name);
            return resource != null ? resource : this.previous.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            var resources = this.trident.findResources(name);
            return resources.hasMoreElements() ? resources : this.previous.getResources(name);
        }
    }
}
