package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IStatStore;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

public interface PixelmonStatsStore<T extends IStatStore> extends UStatsStore<PixelmonStat> {

    T dereference();

    @Override
    default int get(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    @Override
    default void set(UStat<?> stat, int value) {
        if (stat.isPermanent()) {
            this.dereference().setStat((BattleStatsType) stat.dereference(), value);
        }
    }
}
