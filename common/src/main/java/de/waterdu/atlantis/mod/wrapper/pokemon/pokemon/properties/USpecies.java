package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class USpecies<T> extends ModWrapper<T> {

    protected USpecies(ModObjectKey type, T species) {
        super(new ModObject(type, species));
    }

    public abstract String name();

    public abstract int dex();
}
