package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.PokemonStats;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

public interface CobblemonStatsStore<T extends PokemonStats> extends UStatsStore<CobblemonStat> {

    T dereference();

    @Override
    default int get(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getOrDefault((Stat) stat.dereference()) : 0;
    }

    @Override
    default void set(UStat<?> stat, int value) {
        if (stat.isPermanent()) {
            this.dereference().set((Stat) stat.dereference(), value);
        }
    }
}
