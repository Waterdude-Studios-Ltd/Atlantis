package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UIVs<T, S extends UStat<?>> extends ModWrapper<T> implements UStatsStore<S> {

    protected UIVs(ModObjectKey type, T ivs) {
        super(new ModObject(type, ivs));
    }
}
