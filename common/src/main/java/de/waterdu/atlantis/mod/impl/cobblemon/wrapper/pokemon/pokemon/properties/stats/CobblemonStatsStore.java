package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.PokemonStats;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

/**
 * Reads and writes stat values in Cobblemon {@link PokemonStats}, such as EVs and IVs.
 *
 * @param <T> the type of the Cobblemon stats
 */
public interface CobblemonStatsStore<T extends PokemonStats> extends UStatsStore<CobblemonStat> {

    /**
     * @return the wrapped Cobblemon stats
     */
    T dereference();

    /**
     * Gets the value of the given stat.
     *
     * @param stat the stat to get
     * @return the value of the stat, or {@code 0} if the stat is not permanent
     */
    @Override
    default int get(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getOrDefault((Stat) stat.dereference()) : 0;
    }

    /**
     * Sets the value of the given stat. Nothing changes if the stat is not permanent.
     *
     * @param stat the stat to set
     * @param value the new value
     */
    @Override
    default void set(UStat<?> stat, int value) {
        if (stat.isPermanent()) {
            this.dereference().set((Stat) stat.dereference(), value);
        }
    }
}
