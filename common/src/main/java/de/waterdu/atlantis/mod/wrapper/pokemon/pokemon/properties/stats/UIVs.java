package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps the IVs (individual values) of a Pokémon.
 *
 * @param <T> the type of the IVs object of the mod
 * @param <S> the type of the stat wrapper
 */
public abstract class UIVs<T, S extends UStat<?>> extends ModWrapper<T> implements UStatsStore<S> {

    /**
     * Creates a wrapper for the IVs.
     *
     * @param type the key of the mod object type
     * @param ivs the IVs object of the mod
     */
    protected UIVs(ModObjectKey type, T ivs) {
        super(new ModObject(type, ivs));
    }
}
