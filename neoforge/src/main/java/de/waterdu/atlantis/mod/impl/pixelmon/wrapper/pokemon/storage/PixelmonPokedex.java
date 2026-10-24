package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokedex.PokedexStorage;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonBase;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;

import java.util.concurrent.CompletableFuture;

public class PixelmonPokedex extends UPokedex<PokedexStorage> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokedex");

    public PixelmonPokedex(CompletableFuture<PokedexStorage> pokedex) {
        super(TYPE, pokedex);
    }

    @Override
    public boolean hasSeen(USpecies<?> species) {
        return this.isLoaded() && this.dereference().hasSeen(new PokemonBase((Species) species.dereference()));
    }

    @Override
    public boolean hasCaught(USpecies<?> species) {
        return this.isLoaded() && this.dereference().hasCaught(new PokemonBase((Species) species.dereference()));
    }

    @Override
    public int seenCount() {
        return this.isLoaded() ? this.dereference().countSeen() : 0;
    }

    @Override
    public int caughtCount() {
        return this.isLoaded() ? this.dereference().countCaught() : 0;
    }

    @Override
    public void setSeen(UPokemon<?> pokemon) {
        if (this.isLoaded()) {
            this.dereference().setSeen(PixelmonPokemon.unwrap(pokemon), Atlantis.ID);
        }
    }

    @Override
    public void setCaught(UPokemon<?> pokemon) {
        if (this.isLoaded()) {
            this.dereference().setCaught(PixelmonPokemon.unwrap(pokemon), Atlantis.ID);
        }
    }
}
