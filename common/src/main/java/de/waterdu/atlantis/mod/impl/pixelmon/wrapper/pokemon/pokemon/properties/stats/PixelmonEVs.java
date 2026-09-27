package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.EVStore;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;

public class PixelmonEVs extends UEVs<EVStore, PixelmonStat> implements PixelmonStatsStore<EVStore> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "evs");

    public PixelmonEVs(EVStore evs) {
        super(TYPE, evs);
    }
}
