package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps the moveset of a Pokémon. A moveset has up to {@link #SIZE} moves.
 *
 * @param <T> the type of the moveset object of the mod
 * @param <M> the type of the move wrapper
 */
public abstract class UMoveset<T, M extends UMove<?>> extends ModWrapper<T> {

    public static final int SIZE = 4;

    /**
     * Creates a wrapper for the moveset.
     *
     * @param type the key of the mod object type
     * @param moveset the moveset object of the mod
     */
    protected UMoveset(ModObjectKey type, T moveset) {
        super(new ModObject(type, moveset));
    }

    /**
     * @param slot the slot, from 0 to 3
     */
    public abstract @Nullable M get(int slot);

    /**
     * @param slot the slot, from 0 to 3
     * @param move the move to put in the slot, or {@code null} to remove the move in the slot
     */
    public abstract void set(int slot, @Nullable UMoveTemplate<?> move);

    /**
     * Adds the move to the first free slot.
     *
     * @return {@code false} if the moveset is full or already has the move
     */
    public abstract boolean add(UMoveTemplate<?> move);

    /**
     * @param slot the slot, from 0 to 3
     */
    public abstract void remove(int slot);

    /**
     * Removes all moves from the moveset.
     */
    public abstract void clear();

    /**
     * @return the moves in the moveset, without the empty slots
     */
    public abstract List<M> moves();

    /**
     * @return the number of moves in the moveset
     */
    public int size() {
        return this.moves().size();
    }

    /**
     * Checks if the moveset has {@link #SIZE} or more moves.
     *
     * @return {@code true} if no slot is free
     */
    public boolean isFull() {
        return this.size() >= SIZE;
    }

    /**
     * Checks if the moveset has a move with the name of the template. The case of the names is ignored.
     *
     * @param template the template of the move to find
     * @return {@code true} if a move has the same name
     */
    public boolean contains(UMoveTemplate<?> template) {
        for (var move : this.moves()) {
            if (move.name().equalsIgnoreCase(template.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restores the PP of every move in the moveset to its maximum.
     */
    public void restorePP() {
        for (var move : this.moves()) {
            move.restorePP();
        }
    }
}
