package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UBattleResult<T> extends ModWrapper<T> {

    public static final String VICTORY = "victory";
    public static final String DEFEAT = "defeat";
    public static final String DRAW = "draw";
    public static final String FLEE = "flee";

    protected UBattleResult(ModObjectKey type, T result) {
        super(new ModObject(type, result));
    }

    public abstract String name();

    public boolean isVictory() {
        return this.name().equals(VICTORY);
    }

    public boolean isDefeat() {
        return this.name().equals(DEFEAT);
    }

    public boolean isDraw() {
        return this.name().equals(DRAW);
    }

    public boolean isFlee() {
        return this.name().equals(FLEE);
    }

    public boolean is(UBattleResult<?> result) {
        return this.name().equals(result.name());
    }
}
