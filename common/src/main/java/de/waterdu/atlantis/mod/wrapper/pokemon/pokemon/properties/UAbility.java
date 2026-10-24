package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

public abstract class UAbility<T> extends ModWrapper<T> {

    protected UAbility(ModObjectKey type, T ability) {
        super(new ModObject(type, ability));
    }

    public abstract String name();

    public abstract Component displayName();
}
