package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveset;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Pixelmon {@link Moveset}.
 */
public class PixelmonMoveset extends UMoveset<Moveset, PixelmonMove> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "moveset");

    /**
     * Creates a wrapper for the Pixelmon moveset.
     *
     * @param moveset the Pixelmon moveset
     */
    public PixelmonMoveset(Moveset moveset) {
        super(TYPE, moveset);
    }

    /**
     * Gets the move in the slot.
     *
     * @param slot the index of the slot
     * @return the move, or {@code null} if the slot is out of range or empty
     */
    @Override
    public @Nullable PixelmonMove get(int slot) {
        if (slot < 0 || slot >= this.dereference().size()) {
            return null;
        }
        var move = this.dereference().get(slot);
        return move == null ? null : new PixelmonMove(move);
    }

    /**
     * Puts a new move from the template in the slot. If the slot is past the last move, the move is added to the end.
     *
     * @param slot the index of the slot
     * @param move the template of the new move, or {@code null} to remove the move in the slot
     */
    @Override
    public void set(int slot, @Nullable UMoveTemplate<?> move) {
        var moveset = this.dereference();
        if (move == null) {
            this.remove(slot);
        } else if (slot < moveset.size()) {
            moveset.set(slot, new Attack((ImmutableAttack) move.dereference()));
        } else {
            moveset.add(new Attack((ImmutableAttack) move.dereference()));
        }
    }

    /**
     * Adds a new move from the template to the end of the moveset.
     *
     * @param move the template of the new move
     * @return {@code false} if the moveset is full or already has the move
     */
    @Override
    public boolean add(UMoveTemplate<?> move) {
        if (this.isFull() || this.contains(move)) {
            return false;
        }
        return this.dereference().add(new Attack((ImmutableAttack) move.dereference()));
    }

    /**
     * Removes the move in the slot. Does nothing if the slot is out of range.
     *
     * @param slot the index of the slot
     */
    @Override
    public void remove(int slot) {
        if (slot >= 0 && slot < this.dereference().size()) {
            this.dereference().remove(slot);
        }
    }

    /**
     * Removes all moves from the moveset.
     */
    @Override
    public void clear() {
        this.dereference().clear();
    }

    /**
     * Gets wrappers for the moves in the moveset. Empty slots are skipped.
     *
     * @return a new list of the moves
     */
    @Override
    public List<PixelmonMove> moves() {
        var moves = new ArrayList<PixelmonMove>();
        for (var move : this.dereference()) {
            if (move != null) {
                moves.add(new PixelmonMove(move));
            }
        }
        return moves;
    }
}
