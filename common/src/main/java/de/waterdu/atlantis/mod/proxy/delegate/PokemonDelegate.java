package de.waterdu.atlantis.mod.proxy.delegate;

import com.google.gson.JsonElement;
import de.waterdu.atlantis.mod.proxy.ModProxy;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.nbt.CompoundTag;

public interface PokemonDelegate extends ModProxy {

    UParty<?, ?> party(PlayerReference player);

    UPC<?, ?> pc(PlayerReference player);

    UPokemonSpec<?, ?> spec(String... specs);

    String specDelimiter();

    USpecies<?> species(String name);

    USpecies<?> species(int dex);

    UPokemon<?> pokemonFromTag(CompoundTag tag);

    UPokemon<?> pokemonFromJson(JsonElement json);
}
