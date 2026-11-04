package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps a move that a Pokémon knows. It holds the PP state of the move.
 *
 * @param <T> the type of the move object of the mod
 */
public abstract class UMove<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for the move.
     *
     * @param type the key of the mod object type
     * @param move the move object of the mod
     */
    protected UMove(ModObjectKey type, T move) {
        super(new ModObject(type, move));
    }

    /**
     * @return the template that defines the stats of this move
     */
    public abstract UMoveTemplate<?> template();

    /**
     * @return the name of the template of this move
     */
    public String name() {
        return this.template().name();
    }

    /**
     * @return the current PP of the move
     */
    public abstract int pp();

    /**
     * Sets the current PP of the move.
     *
     * @param pp the new PP
     */
    public abstract void setPP(int pp);

    /**
     * @return the maximum PP of the move, with the applied PP Ups
     */
    public abstract int maxPP();

    /**
     * The number of applied PP Ups, from 0 to 3.
     */
    public abstract int ppUps();

    /**
     * Sets the number of applied PP Ups.
     *
     * @param ppUps the number of PP Ups, from 0 to 3
     */
    public abstract void setPPUps(int ppUps);

    /**
     * Sets the current PP to the {@link #maxPP()}.
     */
    public void restorePP() {
        this.setPP(this.maxPP());
    }
}
