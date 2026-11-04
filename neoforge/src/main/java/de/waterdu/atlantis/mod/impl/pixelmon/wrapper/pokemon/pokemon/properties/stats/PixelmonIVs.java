package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IVStore;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;

/**
 * Wraps the Pixelmon {@link IVStore} of a Pokémon.
 */
public class PixelmonIVs extends UIVs<IVStore, PixelmonStat> implements PixelmonStatsStore<IVStore> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "ivs");

    /**
     * Creates a wrapper for the Pixelmon IVs.
     *
     * @param ivs the Pixelmon IV store
     */
    public PixelmonIVs(IVStore ivs) {
        super(TYPE, ivs);
    }
}
