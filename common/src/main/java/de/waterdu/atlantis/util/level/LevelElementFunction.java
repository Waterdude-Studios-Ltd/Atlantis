package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;
import de.waterdu.atlantis.util.java.collection.Pair;
import net.minecraft.resources.ResourceKey;

@FunctionalInterface
public interface LevelElementFunction<R> {

    Pair<ResourceKey<R>, R> apply(AtlantisLevel level, String[] args) throws InvalidAtlantisLevelException;
}
