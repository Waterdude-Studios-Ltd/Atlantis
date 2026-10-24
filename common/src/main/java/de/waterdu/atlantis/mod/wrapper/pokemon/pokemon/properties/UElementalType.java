package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

public abstract class UElementalType<T> extends ModWrapper<T> {

    protected UElementalType(ModObjectKey type, T elementalType) {
        super(new ModObject(type, elementalType));
    }

    public abstract String name();

    public abstract Component displayName();

    public boolean is(UElementalType<?> type) {
        return this.name().equalsIgnoreCase(type.name());
    }
}
