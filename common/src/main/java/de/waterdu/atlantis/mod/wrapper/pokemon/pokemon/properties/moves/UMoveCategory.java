package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

public abstract class UMoveCategory<T> extends ModWrapper<T> {

    public static final String PHYSICAL = "physical";
    public static final String SPECIAL = "special";
    public static final String STATUS = "status";

    protected UMoveCategory(ModObjectKey type, T category) {
        super(new ModObject(type, category));
    }

    public abstract String name();

    public abstract Component displayName();

    public boolean isPhysical() {
        return this.name().equals(PHYSICAL);
    }

    public boolean isSpecial() {
        return this.name().equals(SPECIAL);
    }

    public boolean isStatus() {
        return this.name().equals(STATUS);
    }

    public boolean is(UMoveCategory<?> category) {
        return this.name().equals(category.name());
    }
}
