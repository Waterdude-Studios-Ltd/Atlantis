package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;

public class CobblemonPokedex extends UPokedex<PokedexManager> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokedex");

    public CobblemonPokedex(PokedexManager pokedex) {
        super(TYPE, pokedex);
    }

    @Override
    public boolean hasSeen(USpecies<?> species) {
        return this.knowledge((Species) species.dereference()) != PokedexEntryProgress.UNREGISTERED;
    }

    @Override
    public boolean hasCaught(USpecies<?> species) {
        return this.knowledge((Species) species.dereference()) == PokedexEntryProgress.OWNED;
    }

    @Override
    public int seenCount() {
        return this.count(PokedexEntryProgress.SEEN);
    }

    @Override
    public int caughtCount() {
        return this.count(PokedexEntryProgress.OWNED);
    }

    @Override
    public void setSeen(UPokemon<?> pokemon) {
        this.dereference().encounter(CobblemonPokemon.unwrap(pokemon));
    }

    @Override
    public void setCaught(UPokemon<?> pokemon) {
        this.dereference().obtain(CobblemonPokemon.unwrap(pokemon));
    }

    private PokedexEntryProgress knowledge(Species species) {
        return this.dereference().getKnowledgeForSpecies(species.getResourceIdentifier());
    }

    private int count(PokedexEntryProgress atLeast) {
        var count = 0;
        for (var species : PokemonSpecies.getImplemented()) {
            if (this.knowledge(species).ordinal() >= atLeast.ordinal()) {
                count++;
            }
        }
        return count;
    }
}
