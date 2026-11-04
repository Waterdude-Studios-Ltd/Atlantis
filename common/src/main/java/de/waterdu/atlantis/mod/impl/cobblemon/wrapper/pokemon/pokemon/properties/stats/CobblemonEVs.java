package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.EVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;

/**
 * Wraps the Cobblemon EVs of a Pokémon.
 */
public class CobblemonEVs extends UEVs<EVs, CobblemonStat> implements CobblemonStatsStore<EVs> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "evs");

    /**
     * Creates a wrapper for Cobblemon EVs.
     *
     * @param evs the Cobblemon EVs to wrap
     */
    public CobblemonEVs(EVs evs) {
        super(TYPE, evs);
    }
}
