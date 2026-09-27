package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.pokemon.PokemonStats;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

public interface CobblemonStatsStore<T extends PokemonStats> extends UStatsStore<CobblemonStat> {

    T dereference();

    @Override
    default int get(CobblemonStat stat) {
        return this.dereference().getOrDefault(stat.dereference());
    }

    @Override
    default void set(CobblemonStat stat, int value) {
        this.dereference().set(stat.dereference(), value);
    }
}
