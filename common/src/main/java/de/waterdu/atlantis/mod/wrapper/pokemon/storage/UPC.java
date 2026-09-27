package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;

import java.util.concurrent.CompletableFuture;

public abstract class UPC<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage {

    protected UPC(ModObjectKey type, T pc) {
        super(new ModObject(type, pc));
    }

    protected UPC(ModObjectKey type, CompletableFuture<T> pc) {
        super(new ModFuture(type, pc));
    }

    public abstract P get(int box, int slot);

    public abstract P set(P pokemon, int box, int slot);
}
