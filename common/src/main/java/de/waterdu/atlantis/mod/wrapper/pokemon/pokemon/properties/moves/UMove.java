package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UMove<T> extends ModWrapper<T> {

    protected UMove(ModObjectKey type, T move) {
        super(new ModObject(type, move));
    }

    public abstract UMoveTemplate<?> template();

    public String name() {
        return this.template().name();
    }

    public abstract int pp();

    public abstract void setPP(int pp);

    public abstract int maxPP();

    public void restorePP() {
        this.setPP(this.maxPP());
    }
}
