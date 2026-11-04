package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IStatStore;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStatsStore;

/**
 * Reads and writes stat values in a Pixelmon {@link IStatStore}.
 *
 * @param <T> the type of the Pixelmon stat store
 */
public interface PixelmonStatsStore<T extends IStatStore> extends UStatsStore<PixelmonStat> {

    /**
     * @return the Pixelmon stat store
     */
    T dereference();

    /**
     * Gets the value of the stat from the Pixelmon stat store.
     *
     * @param stat the stat to get the value of
     * @return the value of the stat, or {@code 0} if the stat is not permanent
     */
    @Override
    default int get(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    /**
     * Sets the value of the stat in the Pixelmon stat store. Does nothing if the stat is not permanent.
     *
     * @param stat the stat to set the value of
     * @param value the new value
     */
    @Override
    default void set(UStat<?> stat, int value) {
        if (stat.isPermanent()) {
            this.dereference().setStat((BattleStatsType) stat.dereference(), value);
        }
    }
}
