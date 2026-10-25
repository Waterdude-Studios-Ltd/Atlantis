package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.mod.wrapper.modloader.mod.ModInfo;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public interface TridentHost {

    Path configDir();

    boolean isModLoaded(String modId);

    List<ModInfo> mods();

    default ClassLoader addToClassPath(List<Path> jars) {
        return new URLClassLoader("trident", Trident.jarsToURLs(jars), TridentHost.class.getClassLoader());
    }
}
