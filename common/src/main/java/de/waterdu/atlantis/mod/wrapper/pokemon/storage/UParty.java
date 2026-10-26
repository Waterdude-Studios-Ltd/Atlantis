package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class UParty<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage<P> {

    public static final int SIZE = 6;

    protected UParty(ModObjectKey type, T party) {
        super(new ModObject(type, party));
    }

    protected UParty(ModObjectKey type, CompletableFuture<? extends T> party) {
        super(new ModFuture(type, party));
    }

    @Override
    public boolean isLoaded() {
        return this.dereference() != null;
    }

    @Override
    public int capacity() {
        return SIZE;
    }

    public abstract @Nullable P get(int slot);

    public abstract @Nullable P set(int slot, @Nullable UPokemon<?> pokemon);

    public abstract void swap(int slot1, int slot2);

    public abstract void heal();

    /**
     * Recalls every Pokémon from the party that is currently sent out.
     */
    public abstract void retrieveAll();

    public int countAble() {
        var count = 0;
        for (var pokemon : this.all()) {
            if (!pokemon.isEgg() && !pokemon.isFainted()) {
                count++;
            }
        }
        return count;
    }

    public @Nullable P firstAble() {
        for (var pokemon : this.all()) {
            if (!pokemon.isEgg() && !pokemon.isFainted()) {
                return pokemon;
            }
        }
        return null;
    }
}
