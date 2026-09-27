package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.api.pokemon.stats.IStatStore;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

public interface PixelmonStatsStore<T extends IStatStore> extends UStatsStore<PixelmonStat> {

    T dereference();

    @Override
    default int get(PixelmonStat stat) {
        return this.dereference().getStat(stat.dereference());
    }

    @Override
    default void set(PixelmonStat stat, int value) {
        this.dereference().setStat(stat.dereference(), value);
    }
}
