package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMove;

public class PixelmonMove extends UMove<Attack> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "move");

    public PixelmonMove(Attack move) {
        super(TYPE, move);
    }

    @Override
    public PixelmonMoveTemplate template() {
        return new PixelmonMoveTemplate(this.dereference().getMove());
    }

    @Override
    public int pp() {
        return this.dereference().getPP();
    }

    @Override
    public void setPP(int pp) {
        this.dereference().setPP(Math.clamp(pp, 0, this.maxPP()));
    }

    @Override
    public int maxPP() {
        return this.dereference().getMaxPP();
    }

    @Override
    public int ppUps() {
        return this.dereference().getPPLevel();
    }

    @Override
    public void setPPUps(int ppUps) {
        this.dereference().setPPLevel(Math.clamp(ppUps, 0, 3));
    }
}
