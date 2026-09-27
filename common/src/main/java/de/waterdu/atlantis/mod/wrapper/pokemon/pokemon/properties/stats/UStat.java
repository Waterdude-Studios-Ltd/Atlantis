package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UStat<T> extends ModWrapper<T> {

    protected UStat(ModObjectKey type, T stat) {
        super(new ModObject(type, stat));
    }
}
