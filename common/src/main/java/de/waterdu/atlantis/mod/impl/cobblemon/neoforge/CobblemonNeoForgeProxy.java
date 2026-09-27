package de.waterdu.atlantis.mod.impl.cobblemon.neoforge;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonElement;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.parsing.CobblemonPokemonSpec;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.properties.CobblemonSpecies;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.storage.CobblemonPC;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.storage.CobblemonParty;
import de.waterdu.atlantis.mod.proxy.delegate.PokemonDelegate;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.nbt.CompoundTag;

public class CobblemonNeoForgeProxy implements PokemonDelegate {

    private final Species bulbasaur = PokemonSpecies.getByName("Bulbasaur");
    private final PokemonStoreManager storage = Cobblemon.INSTANCE.getStorage();

    @Override
    public void setup() {

    }

    @Override
    public CobblemonParty party(PlayerReference player) {
        return new CobblemonParty(this.storage.getParty(player.uuid(), ServerUtils.registryAccess()));
    }

    @Override
    public CobblemonPC pc(PlayerReference player) {
        return new CobblemonPC(this.storage.getPC(player.uuid(), ServerUtils.registryAccess()));
    }

    @Override
    public CobblemonPokemonSpec spec(String... specs) {
        return new CobblemonPokemonSpec(PokemonProperties.Companion.parse(TextUtils.concat(specs)));
    }

    @Override
    public String specDelimiter() {
        return "=";
    }

    @Override
    public CobblemonSpecies species(String name) {
        var species = PokemonSpecies.getByName(name);
        if (species == null) {
            species = this.bulbasaur;
        }
        return new CobblemonSpecies(species);
    }

    @Override
    public CobblemonSpecies species(int dex) {
        var species = PokemonSpecies.getByPokedexNumber(dex, Cobblemon.MODID);
        if (species == null) {
            species = this.bulbasaur;
        }
        return new CobblemonSpecies(species);
    }

    @Override
    public CobblemonPokemon pokemonFromTag(CompoundTag tag) {
        Pokemon pokemon;
        try {
            pokemon = Pokemon.Companion.loadFromNBT(ServerUtils.registryAccess(), tag);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to parse Cobblemon Pokémon data from tag: {}, {}", e.getMessage(), tag);
            pokemon = this.bulbasaur.create(5);
        }
        return new CobblemonPokemon(pokemon);
    }

    @Override
    public CobblemonPokemon pokemonFromJson(JsonElement json) {
        var nbt = json.getAsString();
        Pokemon pokemon;
        try {
            pokemon = Pokemon.Companion.loadFromJSON(ServerUtils.registryAccess(), json.getAsJsonObject());
        } catch (Exception e) {
            AtlantisLogger.error("Failed to parse Cobblemon Pokémon data from json: {}, {}", e.getMessage(), nbt);
            pokemon = this.bulbasaur.create(5);
        }
        return new CobblemonPokemon(pokemon);
    }
}
