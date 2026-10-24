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

public class CobblemonMoveset extends UMoveset<MoveSet, CobblemonMove> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "moveset");

    public CobblemonMoveset(MoveSet moveset) {
        super(TYPE, moveset);
    }

    @Override
    public @Nullable CobblemonMove get(int slot) {
        var move = this.dereference().get(slot);
        return move == null ? null : new CobblemonMove(move);
    }

    @Override
    public void set(int slot, @Nullable UMoveTemplate<?> move) {
        this.dereference().setMove(slot, move == null ? null : ((MoveTemplate) move.dereference()).create());
    }

    @Override
    public boolean add(UMoveTemplate<?> move) {
        if (this.isFull() || this.contains(move)) {
            return false;
        }
        return this.dereference().add(((MoveTemplate) move.dereference()).create());
    }

    @Override
    public void remove(int slot) {
        this.dereference().setMove(slot, null);
    }

    @Override
    public void clear() {
        this.dereference().clear();
    }

    @Override
    public List<CobblemonMove> moves() {
        var moves = new ArrayList<CobblemonMove>();
        for (var move : this.dereference().getMoves()) {
            moves.add(new CobblemonMove(move));
        }
        return moves;
    }
}
