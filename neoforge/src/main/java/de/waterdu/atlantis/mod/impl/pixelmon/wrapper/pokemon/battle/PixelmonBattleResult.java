package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleResult;

import java.util.Locale;

public class PixelmonBattleResult extends UBattleResult<BattleResults> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_result");

    public PixelmonBattleResult(BattleResults result) {
        super(TYPE, result);
    }

    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }
}
