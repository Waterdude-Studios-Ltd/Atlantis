package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

/**
 * Holds one value for each stat, such as the EVs or IVs of a Pokémon.
 *
 * @param <S> the type of the stat wrapper
 */
public interface UStatsStore<S extends UStat<?>> {

    /**
     * Gets the value of the stat.
     *
     * @param stat the stat to get the value of
     * @return the value of the stat
     */
    int get(UStat<?> stat);

    /**
     * Sets the value of the stat.
     *
     * @param stat the stat to set the value of
     * @param value the new value
     */
    void set(UStat<?> stat, int value);
}
