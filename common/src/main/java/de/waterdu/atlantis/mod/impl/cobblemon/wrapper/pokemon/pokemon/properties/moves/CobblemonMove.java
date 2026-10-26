package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMove;

public class CobblemonMove extends UMove<Move> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move");

    public CobblemonMove(Move move) {
        super(TYPE, move);
    }

    @Override
    public CobblemonMoveTemplate template() {
        return new CobblemonMoveTemplate(this.dereference().getTemplate());
    }

    @Override
    public int pp() {
        return this.dereference().getCurrentPp();
    }

    @Override
    public void setPP(int pp) {
        this.dereference().setCurrentPp(Math.clamp(pp, 0, this.maxPP()));
    }

    @Override
    public int maxPP() {
        return this.dereference().getMaxPp();
    }

    @Override
    public int ppUps() {
        return this.dereference().getRaisedPpStages();
    }

    @Override
    public void setPPUps(int ppUps) {
        this.dereference().setRaisedPpStages(Math.clamp(ppUps, 0, 3));
    }
}
