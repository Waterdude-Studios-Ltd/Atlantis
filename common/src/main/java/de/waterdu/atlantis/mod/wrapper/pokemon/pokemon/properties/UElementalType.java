package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UElementalType<T> extends ModWrapper<T> {

    protected UElementalType(ModObjectKey type, T elementalType) {
        super(new ModObject(type, elementalType));
    }
}
