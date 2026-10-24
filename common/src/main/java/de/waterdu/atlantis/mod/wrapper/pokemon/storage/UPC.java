package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class UPC<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage<P> {

    protected UPC(ModObjectKey type, T pc) {
        super(new ModObject(type, pc));
    }

    protected UPC(ModObjectKey type, CompletableFuture<T> pc) {
        super(new ModFuture(type, pc));
    }

    @Override
    public boolean isLoaded() {
        return this.dereference() != null;
    }

    @Override
    public int capacity() {
        return this.boxCount() * this.boxSize();
    }

    public abstract @Nullable P get(int box, int slot);

    public abstract @Nullable P set(@Nullable UPokemon<?> pokemon, int box, int slot);

    public abstract int boxCount();

    public abstract int boxSize();

    public List<P> box(int box) {
        var pokemon = new ArrayList<P>();
        for (var slot = 0; slot < this.boxSize(); slot++) {
            var found = this.get(box, slot);
            if (found != null) {
                pokemon.add(found);
            }
        }
        return pokemon;
    }
}
