package de.waterdu.atlantis.level.level;

import net.minecraft.resources.ResourceLocation;

public class InvalidAtlantisLevelException extends Exception {

    protected InvalidAtlantisLevelException(AtlantisLevel level, String type, String key) {
        super("Invalid property: " + type + " " + key + " (" + level.toString() + ")");
    }

    protected InvalidAtlantisLevelException(String type, ResourceLocation key, Exception e) {
        super("Construction error: " + type + " " + key.toString() + ": " + e.getMessage());
    }
}
