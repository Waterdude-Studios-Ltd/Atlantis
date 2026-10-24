package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;

import java.util.concurrent.CompletableFuture;

public abstract class UPokedex<T> extends ModWrapper<T> {

    protected UPokedex(ModObjectKey type, T pokedex) {
        super(new ModObject(type, pokedex));
    }

    protected UPokedex(ModObjectKey type, CompletableFuture<? extends T> pokedex) {
        super(new ModFuture(type, pokedex));
    }

    public boolean isLoaded() {
        return this.dereference() != null;
    }

    public abstract boolean hasSeen(USpecies<?> species);

    public abstract boolean hasCaught(USpecies<?> species);

    public abstract int seenCount();

    public abstract int caughtCount();

    public abstract void setSeen(UPokemon<?> pokemon);

    public abstract void setCaught(UPokemon<?> pokemon);
}
