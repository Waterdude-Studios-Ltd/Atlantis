package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleResult;

/**
 * Holds a battle result for Cobblemon. Cobblemon has no result type, so each result wraps its Atlantis name.
 */
public class CobblemonBattleResult extends UBattleResult<String> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle_result");

    public static final CobblemonBattleResult VICTORY_RESULT = new CobblemonBattleResult(VICTORY);
    public static final CobblemonBattleResult DEFEAT_RESULT = new CobblemonBattleResult(DEFEAT);
    public static final CobblemonBattleResult DRAW_RESULT = new CobblemonBattleResult(DRAW);
    public static final CobblemonBattleResult FLEE_RESULT = new CobblemonBattleResult(FLEE);

    /**
     * Creates a result with the given name.
     *
     * @param result the name of the result
     */
    private CobblemonBattleResult(String result) {
        super(TYPE, result);
    }

    /**
     * @return the name of the result
     */
    @Override
    public String name() {
        return this.dereference();
    }
}
