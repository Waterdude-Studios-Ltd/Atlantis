package de.waterdu.atlantis.loader;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AtlantisModLoader {

    private static final Map<String, Object> MODS = new ConcurrentHashMap<>();

    private AtlantisModLoader() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void load() {
    }
}
