package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveset;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Cobblemon move set.
 */
public class CobblemonMoveset extends UMoveset<MoveSet, CobblemonMove> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "moveset");

    /**
     * Creates a wrapper for a Cobblemon move set.
     *
     * @param moveset the Cobblemon move set to wrap
     */
    public CobblemonMoveset(MoveSet moveset) {
        super(TYPE, moveset);
    }

    /**
     * Gets the move in the given slot.
     *
     * @param slot the slot of the move
     * @return the move, or {@code null} if the slot is empty
     */
    @Override
    public @Nullable CobblemonMove get(int slot) {
        var move = this.dereference().get(slot);
        return move == null ? null : new CobblemonMove(move);
    }

    /**
     * Puts a new move from the given template into a slot.
     *
     * @param slot the slot to change
     * @param move the template of the new move, or {@code null} to empty the slot
     */
    @Override
    public void set(int slot, @Nullable UMoveTemplate<?> move) {
        this.dereference().setMove(slot, move == null ? null : ((MoveTemplate) move.dereference()).create());
    }

    /**
     * Adds a new move from the given template.
     *
     * @param move the template of the new move
     * @return {@code false} if the move set is full or already has the move, or if Cobblemon does not add it
     */
    @Override
    public boolean add(UMoveTemplate<?> move) {
        if (this.isFull() || this.contains(move)) {
            return false;
        }
        return this.dereference().add(((MoveTemplate) move.dereference()).create());
    }

    /**
     * Empties the given slot.
     *
     * @param slot the slot to empty
     */
    @Override
    public void remove(int slot) {
        this.dereference().setMove(slot, null);
    }

    /**
     * Removes all moves from this move set.
     */
    @Override
    public void clear() {
        this.dereference().clear();
    }

    /**
     * Gets the moves in this move set.
     *
     * @return a new list of the moves
     */
    @Override
    public List<CobblemonMove> moves() {
        var moves = new ArrayList<CobblemonMove>();
        for (var move : this.dereference().getMoves()) {
            moves.add(new CobblemonMove(move));
        }
        return moves;
    }
}
