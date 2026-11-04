package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps the EVs (effort values) of a Pokémon.
 *
 * @param <T> the type of the EVs object of the mod
 * @param <S> the type of the stat wrapper
 */
public abstract class UEVs<T, S extends UStat<?>> extends ModWrapper<T> implements UStatsStore<S> {

    /**
     * Creates a wrapper for the EVs.
     *
     * @param type the key of the mod object type
     * @param evs the EVs object of the mod
     */
    protected UEVs(ModObjectKey type, T evs) {
        super(new ModObject(type, evs));
    }
}
