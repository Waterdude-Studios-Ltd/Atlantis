package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.EVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;

public class CobblemonEVs extends UEVs<EVs, CobblemonStat> implements CobblemonStatsStore<EVs> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "evs");

    public CobblemonEVs(EVs evs) {
        super(TYPE, evs);
    }
}
