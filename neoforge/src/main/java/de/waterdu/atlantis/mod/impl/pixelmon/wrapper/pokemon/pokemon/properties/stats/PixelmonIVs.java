package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IVStore;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;

public class PixelmonIVs extends UIVs<IVStore, PixelmonStat> implements PixelmonStatsStore<IVStore> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "ivs");

    public PixelmonIVs(IVStore ivs) {
        super(TYPE, ivs);
    }
}
