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

public class PixelmonMoveset extends UMoveset<Moveset, PixelmonMove> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "moveset");

    public PixelmonMoveset(Moveset moveset) {
        super(TYPE, moveset);
    }

    @Override
    public @Nullable PixelmonMove get(int slot) {
        if (slot < 0 || slot >= this.dereference().size()) {
            return null;
        }
        var move = this.dereference().get(slot);
        return move == null ? null : new PixelmonMove(move);
    }

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

    @Override
    public boolean add(UMoveTemplate<?> move) {
        if (this.isFull() || this.contains(move)) {
            return false;
        }
        return this.dereference().add(new Attack((ImmutableAttack) move.dereference()));
    }

    @Override
    public void remove(int slot) {
        if (slot >= 0 && slot < this.dereference().size()) {
            this.dereference().remove(slot);
        }
    }

    @Override
    public void clear() {
        this.dereference().clear();
    }

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
