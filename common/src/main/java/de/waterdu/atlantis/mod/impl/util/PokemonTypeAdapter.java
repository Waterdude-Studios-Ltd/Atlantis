package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;

import java.lang.reflect.Type;

/**
 * Reads and writes a {@link UPokemon} as JSON through the Pokémon delegate.
 */
public class PokemonTypeAdapter implements JsonDeserializer<UPokemon<?>>, JsonSerializer<UPokemon<?>> {

    /**
     * Reads a Pokémon from JSON with the Pokémon delegate.
     *
     * @param json the JSON of the Pokémon
     * @param typeOfT the type to read
     * @param context the deserialization context
     * @return the Pokémon
     */
    @Override
    public UPokemon<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Mods.pokemon().pokemonFromJson(json);
    }

    /**
     * Writes the Pokémon as JSON with {@link UPokemon#toJson()}.
     *
     * @param src the Pokémon
     * @param typeOfSrc the type to write
     * @param context the serialization context
     * @return the JSON of the Pokémon
     */
    @Override
    public JsonElement serialize(UPokemon<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return src.toJson();
    }
}