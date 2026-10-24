package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import net.minecraft.network.chat.Component;

public abstract class UMoveTemplate<T> extends ModWrapper<T> {

    protected UMoveTemplate(ModObjectKey type, T move) {
        super(new ModObject(type, move));
    }

    public abstract String name();

    public abstract Component displayName();

    public abstract UElementalType<?> elementalType();

    public abstract UMoveCategory<?> category();

    public abstract int power();

    public abstract int accuracy();

    public abstract int basePP();

    public abstract int maxPP();
}
