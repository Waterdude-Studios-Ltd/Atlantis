package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.List;

public abstract class UForm<T> extends ModWrapper<T> {

    protected UForm(ModObjectKey type, T form) {
        super(new ModObject(type, form));
    }

    public abstract String name();

    public abstract USpecies<?> species();

    public abstract List<? extends UElementalType<?>> types();

    public abstract int baseStat(UStat<?> stat);

    public abstract int baseStatTotal();

    public abstract int catchRate();

    public boolean hasType(UElementalType<?> type) {
        for (var elementalType : this.types()) {
            if (elementalType.is(type)) {
                return true;
            }
        }
        return false;
    }
}
