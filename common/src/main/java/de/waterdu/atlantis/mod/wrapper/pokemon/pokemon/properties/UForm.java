package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

import java.util.List;

public abstract class UForm<T> extends ModWrapper<T> {

    protected UForm(ModObjectKey type, T form) {
        super(new ModObject(type, form));
    }

    public abstract List<? extends UElementalType<?>> types();
}
