package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.java.UUIDUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.lang.reflect.Type;
import java.util.UUID;

public class StoredPokemon {

    private CompoundTag tag;
    private UPokemon<?> pokemon;

    private StoredPokemon() {
        this((UPokemon<?>) null);
    }

    private StoredPokemon(UPokemon<?> pokemon) {
        this.set(pokemon);
    }

    private StoredPokemon(CompoundTag tag) {
        this.tag = tag;
    }

    private StoredPokemon(String... specs) {
        this(Mods.pokemon().spec(specs));
    }

    private StoredPokemon(UPokemonSpec<?, ?> spec) {
        this(spec.create());
    }

    private StoredPokemon(ConfigurablePokemon spec) {
        this(spec.instance());
    }

    public static StoredPokemon empty() {
        return new StoredPokemon();
    }

    public static StoredPokemon of(UPokemon<?> pokemon) {
        return new StoredPokemon(pokemon);
    }

    public static StoredPokemon of(CompoundTag tag) {
        return new StoredPokemon(tag);
    }

    public static StoredPokemon of(String spec) {
        return new StoredPokemon(spec);
    }

    public static StoredPokemon of(UPokemonSpec<?, ?> spec) {
        return new StoredPokemon(spec);
    }

    public static StoredPokemon of(ConfigurablePokemon spec) {
        return new StoredPokemon(spec);
    }

    public void set(UPokemon<?> pokemon) {
        if (pokemon == null) {
            this.tag = null;
            this.pokemon = null;
        } else {
            this.tag = pokemon.toCompoundTag();
            this.pokemon = pokemon;
        }
    }

    public void clear() {
        this.set(null);
    }

    public UPokemon<?> actual() {
        if (this.isEmpty()) {
            return null;
        }

        if (this.pokemon == null) {
            this.pokemon = Mods.pokemon().pokemonFromTag(this.tag);
        }

        return this.pokemon;
    }

    public boolean isEmpty() {
        return this.tag == null || this.tag.isEmpty();
    }

    public UUID uuid() {
        var actual = this.actual();
        return actual == null ? UUIDUtils.ZEROED_UUID : actual.uuid();
    }

    @Override
    public int hashCode() {
        return this.uuid().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StoredPokemon)) {
            return false;
        }
        return this.uuid().equals(((StoredPokemon) obj).uuid());
    }

    @Override
    public String toString() {
        return "StoredPokemon(" + this.uuid() + ")";
    }

    public static class TypeAdapter implements JsonSerializer<StoredPokemon>, JsonDeserializer<StoredPokemon> {

        @Override
        public StoredPokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return new StoredPokemon();
            } else {
                try {
                    return new StoredPokemon(TagParser.parseTag(json.getAsString()));
                } catch (CommandSyntaxException e) {
                    AtlantisLogger.error("Failed to parse StoredPokemon tag: {}", e.getMessage());
                    return new StoredPokemon();
                }
            }
        }

        @Override
        public JsonElement serialize(StoredPokemon src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.tag.toString());
        }
    }

    public static StoredPokemon[] emptyArrayOfSize(int size) {
        var pokemon = new StoredPokemon[size];
        for (var i = 0; i < pokemon.length; i++) {
            pokemon[i] = new StoredPokemon();
        }
        return pokemon;
    }
}
