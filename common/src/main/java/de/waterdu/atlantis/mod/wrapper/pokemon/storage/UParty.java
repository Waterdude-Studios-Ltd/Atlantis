package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;

import java.util.concurrent.CompletableFuture;

public abstract class UParty<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage {

    protected UParty(ModObjectKey type, T party) {
        super(new ModObject(type, party));
    }

    protected UParty(ModObjectKey type, CompletableFuture<? extends T> party) {
        super(new ModFuture(type, party));
    }

    public abstract P get(int slot);

    public abstract P set(int slot, P pokemon);
}
