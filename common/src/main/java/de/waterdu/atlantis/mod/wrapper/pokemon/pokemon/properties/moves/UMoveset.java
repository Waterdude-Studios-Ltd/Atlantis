package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class UMoveset<T, M extends UMove<?>> extends ModWrapper<T> {

    public static final int SIZE = 4;

    protected UMoveset(ModObjectKey type, T moveset) {
        super(new ModObject(type, moveset));
    }

    public abstract @Nullable M get(int slot);

    public abstract void set(int slot, @Nullable UMoveTemplate<?> move);

    public abstract boolean add(UMoveTemplate<?> move);

    public abstract void remove(int slot);

    public abstract void clear();

    public abstract List<M> moves();

    public int size() {
        return this.moves().size();
    }

    public boolean isFull() {
        return this.size() >= SIZE;
    }

    public boolean contains(UMoveTemplate<?> template) {
        for (var move : this.moves()) {
            if (move.name().equalsIgnoreCase(template.name())) {
                return true;
            }
        }
        return false;
    }

    public void restorePP() {
        for (var move : this.moves()) {
            move.restorePP();
        }
    }
}
