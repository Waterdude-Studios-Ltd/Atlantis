package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;

@FunctionalInterface
public interface AnonymousLevelElementFunction<R> {

    R apply(AtlantisLevel level, String[] args) throws InvalidAtlantisLevelException;
}
