package de.waterdu.atlantis.nereus;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Due to the NeoForge/Fabric proxies present within Atlantis, mods that depend on Atlantis don't strictly need to know
 * about either launcher.
 * As such, we want to manage the loading of such mods ourselves.
 * This loading functionality is called "Nereus", with an individual mod a "Nereid".
 * These JARs should be placed within a configured location as determined by AtlantisConfig, defaulting to
 * "./config/atlantis/nereus".
 * These should be loaded before anything else, so they can still register lifecycle listeners via loader proxies and
 * set up anything they need to.
 */
public class NereusLoader {

    private static final Map<String, Object> MODS = new ConcurrentHashMap<>();

    private NereusLoader() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void load() {
    }
}
