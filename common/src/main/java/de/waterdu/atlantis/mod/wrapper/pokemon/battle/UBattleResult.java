package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps the result of a battle from the Pokémon mod.
 *
 * @param <T> the type of the wrapped result
 */
public abstract class UBattleResult<T> extends ModWrapper<T> {

    public static final String VICTORY = "victory";
    public static final String DEFEAT = "defeat";
    public static final String DRAW = "draw";
    public static final String FLEE = "flee";

    /**
     * Creates a wrapper for a battle result.
     *
     * @param type the key of the mod object type
     * @param result the result to wrap
     */
    protected UBattleResult(ModObjectKey type, T result) {
        super(new ModObject(type, result));
    }

    /**
     * @return the name of the result, such as {@link #VICTORY} or {@link #FLEE}
     */
    public abstract String name();

    /**
     * Checks if the result is a victory.
     *
     * @return {@code true} if the name is {@link #VICTORY}
     */
    public boolean isVictory() {
        return this.name().equals(VICTORY);
    }

    /**
     * Checks if the result is a defeat.
     *
     * @return {@code true} if the name is {@link #DEFEAT}
     */
    public boolean isDefeat() {
        return this.name().equals(DEFEAT);
    }

    /**
     * Checks if the result is a draw.
     *
     * @return {@code true} if the name is {@link #DRAW}
     */
    public boolean isDraw() {
        return this.name().equals(DRAW);
    }

    /**
     * Checks if the result is a flee.
     *
     * @return {@code true} if the name is {@link #FLEE}
     */
    public boolean isFlee() {
        return this.name().equals(FLEE);
    }

    /**
     * Checks if this result has the same name as another result.
     *
     * @param result the result to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UBattleResult<?> result) {
        return this.name().equals(result.name());
    }
}
