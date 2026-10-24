package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

public interface UStatsStore<S extends UStat<?>> {

    int get(UStat<?> stat);

    void set(UStat<?> stat, int value);
}
