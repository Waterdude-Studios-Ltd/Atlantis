package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

public class PixelmonStat extends UStat<BattleStatsType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "stat");

    public PixelmonStat(BattleStatsType stat) {
        super(TYPE, stat);
    }
}
