package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

public interface UStatsStore<S extends UStat<?>> {

    int get(S stat);

    void set(S stat, int value);
}
