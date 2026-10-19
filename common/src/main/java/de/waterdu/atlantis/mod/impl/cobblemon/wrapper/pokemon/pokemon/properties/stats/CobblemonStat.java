package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

public class CobblemonStat extends UStat<Stat> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "stat");

    public CobblemonStat(Stat stat) {
        super(TYPE, stat);
    }
}
