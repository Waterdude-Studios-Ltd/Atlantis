package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

/**
 * Wraps a move category of a Pokémon mod: physical, special or status.
 *
 * @param <T> the type of the move category object of the mod
 */
public abstract class UMoveCategory<T> extends ModWrapper<T> {

    public static final String PHYSICAL = "physical";
    public static final String SPECIAL = "special";
    public static final String STATUS = "status";

    /**
     * Creates a wrapper for the move category.
     *
     * @param type the key of the mod object type
     * @param category the move category object of the mod
     */
    protected UMoveCategory(ModObjectKey type, T category) {
        super(new ModObject(type, category));
    }

    /**
     * @return the name of the category, such as {@link #PHYSICAL}
     */
    public abstract String name();

    /**
     * @return the name of the category to show to players
     */
    public abstract Component displayName();

    /**
     * Checks if this is the physical category.
     *
     * @return {@code true} if the name is {@link #PHYSICAL}
     */
    public boolean isPhysical() {
        return this.name().equals(PHYSICAL);
    }

    /**
     * Checks if this is the special category.
     *
     * @return {@code true} if the name is {@link #SPECIAL}
     */
    public boolean isSpecial() {
        return this.name().equals(SPECIAL);
    }

    /**
     * Checks if this is the status category.
     *
     * @return {@code true} if the name is {@link #STATUS}
     */
    public boolean isStatus() {
        return this.name().equals(STATUS);
    }

    /**
     * Checks if this category has the same name as the other category.
     *
     * @param category the category to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UMoveCategory<?> category) {
        return this.name().equals(category.name());
    }
}
