package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.IVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;

public class CobblemonIVs extends UIVs<IVs, CobblemonStat> implements CobblemonStatsStore<IVs> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "ivs");

    public CobblemonIVs(IVs ivs) {
        super(TYPE, ivs);
    }
}
