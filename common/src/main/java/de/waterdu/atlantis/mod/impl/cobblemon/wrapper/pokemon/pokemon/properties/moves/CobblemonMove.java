package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMove;

/**
 * Wraps a Cobblemon move that a Pokémon knows.
 */
public class CobblemonMove extends UMove<Move> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move");

    /**
     * Creates a wrapper for a Cobblemon move.
     *
     * @param move the Cobblemon move to wrap
     */
    public CobblemonMove(Move move) {
        super(TYPE, move);
    }

    /**
     * @return a new wrapper for the template of the move
     */
    @Override
    public CobblemonMoveTemplate template() {
        return new CobblemonMoveTemplate(this.dereference().getTemplate());
    }

    /**
     * @return the current PP of the move
     */
    @Override
    public int pp() {
        return this.dereference().getCurrentPp();
    }

    /**
     * Sets the current PP of this move. The value is clamped between 0 and {@link #maxPP()}.
     *
     * @param pp the new PP
     */
    @Override
    public void setPP(int pp) {
        this.dereference().setCurrentPp(Math.clamp(pp, 0, this.maxPP()));
    }

    /**
     * @return the maximum PP of the move, with the PP Ups applied
     */
    @Override
    public int maxPP() {
        return this.dereference().getMaxPp();
    }

    /**
     * @return the number of PP Ups applied to the move
     */
    @Override
    public int ppUps() {
        return this.dereference().getRaisedPpStages();
    }

    /**
     * Sets the number of PP Ups applied to this move. The value is clamped between 0 and 3.
     *
     * @param ppUps the new number of PP Ups
     */
    @Override
    public void setPPUps(int ppUps) {
        this.dereference().setRaisedPpStages(Math.clamp(ppUps, 0, 3));
    }
}
