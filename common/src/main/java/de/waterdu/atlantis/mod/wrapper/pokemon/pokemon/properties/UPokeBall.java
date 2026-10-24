package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public abstract class UPokeBall<T> extends ModWrapper<T> {

    protected UPokeBall(ModObjectKey type, T ball) {
        super(new ModObject(type, ball));
    }

    public abstract String name();

    public abstract Component displayName();

    public abstract ItemStack item();
}
