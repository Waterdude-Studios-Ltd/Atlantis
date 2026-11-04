package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMove;

/**
 * Wraps a Pixelmon move that a Pokémon knows.
 */
public class PixelmonMove extends UMove<Attack> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "move");

    /**
     * Creates a wrapper for a Pixelmon move.
     *
     * @param move the move to wrap
     */
    public PixelmonMove(Attack move) {
        super(TYPE, move);
    }

    /**
     * @return a new wrapper for the template of the move
     */
    @Override
    public PixelmonMoveTemplate template() {
        return new PixelmonMoveTemplate(this.dereference().getMove());
    }

    /**
     * @return the current PP of the move
     */
    @Override
    public int pp() {
        return this.dereference().getPP();
    }

    /**
     * Sets the current PP of the move, clamped between {@code 0} and {@link #maxPP()}.
     *
     * @param pp the new PP
     */
    @Override
    public void setPP(int pp) {
        this.dereference().setPP(Math.clamp(pp, 0, this.maxPP()));
    }

    /**
     * @return the maximum PP of the move
     */
    @Override
    public int maxPP() {
        return this.dereference().getMaxPP();
    }

    /**
     * @return the number of PP Ups applied to the move
     */
    @Override
    public int ppUps() {
        return this.dereference().getPPLevel();
    }

    /**
     * Sets the number of PP Ups applied to the move, clamped between {@code 0} and {@code 3}.
     *
     * @param ppUps the new number of PP Ups
     */
    @Override
    public void setPPUps(int ppUps) {
        this.dereference().setPPLevel(Math.clamp(ppUps, 0, 3));
    }
}
