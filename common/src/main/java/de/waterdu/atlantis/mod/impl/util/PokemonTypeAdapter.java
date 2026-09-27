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

public class PokemonTypeAdapter implements JsonDeserializer<UPokemon<?>>, JsonSerializer<UPokemon<?>> {

    @Override
    public UPokemon<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Mods.pokemon().pokemonFromJson(json);
    }

    @Override
    public JsonElement serialize(UPokemon<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return src.toJson();
    }
}