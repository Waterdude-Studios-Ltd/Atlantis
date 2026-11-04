package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import net.minecraft.network.chat.Component;

/**
 * Wraps the definition of a move in a Pokémon mod. A {@link UMove} is an instance of a template that a Pokémon knows.
 *
 * @param <T> the type of the move template object of the mod
 */
public abstract class UMoveTemplate<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for the move template.
     *
     * @param type the key of the mod object type
     * @param move the move template object of the mod
     */
    protected UMoveTemplate(ModObjectKey type, T move) {
        super(new ModObject(type, move));
    }

    /**
     * @return the name of the move
     */
    public abstract String name();

    /**
     * @return the name of the move to show to players
     */
    public abstract Component displayName();

    /**
     * @return the elemental type of the move
     */
    public abstract UElementalType<?> elementalType();

    /**
     * @return the category of the move
     */
    public abstract UMoveCategory<?> category();

    /**
     * @return the base power of the move
     */
    public abstract int power();

    /**
     * @return the accuracy of the move
     */
    public abstract int accuracy();

    /**
     * @return the PP of the move without PP Ups
     */
    public abstract int basePP();

    /**
     * @return the maximum PP of the move
     */
    public abstract int maxPP();
}
