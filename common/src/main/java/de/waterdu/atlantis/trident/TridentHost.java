package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * Gives {@link Trident} the parts of the mod loader that it needs to discover and load Tines.
 */
public interface TridentHost {

    /**
     * @return the config directory of the mod loader
     */
    Path configDir();

    /**
     * Checks if a mod with the ID is loaded.
     *
     * @param modId the ID of the mod
     * @return {@code true} if the mod is loaded
     */
    boolean isModLoaded(String modId);

    /**
     * @return the info of the loaded mods
     */
    List<ModInfo> mods();

    /**
     * Makes the classes in the jars and directories loadable. By default, this creates a new class loader whose parent
     * is the class loader of Atlantis.
     *
     * @return the class loader to load the Tine classes with
     */
    default ClassLoader addToClassPath(List<Path> jars) {
        return new URLClassLoader("trident", Trident.jarsToURLs(jars), TridentHost.class.getClassLoader());
    }
}
