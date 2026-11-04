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

/**
 * Stores a Pokémon as a tag, so that it can be saved in configs and data files. The Pokémon is read from the tag on
 * first use.
 */
public class StoredPokemon {

    private CompoundTag tag;
    private UPokemon<?> pokemon;

    /**
     * Creates an empty stored Pokémon.
     */
    private StoredPokemon() {
        this((UPokemon<?>) null);
    }

    /**
     * Creates a stored Pokémon with a copy of the Pokémon as a tag.
     *
     * @param pokemon the Pokémon to store, or {@code null} for an empty stored Pokémon
     */
    private StoredPokemon(UPokemon<?> pokemon) {
        this.set(pokemon);
    }

    /**
     * Creates a stored Pokémon from a tag. The Pokémon is read from the tag on first use.
     *
     * @param tag the tag of the Pokémon
     */
    private StoredPokemon(CompoundTag tag) {
        this.tag = tag;
    }

    /**
     * Creates a stored Pokémon with a new Pokémon made from a spec.
     *
     * @param specs the parts of the spec
     */
    private StoredPokemon(String... specs) {
        this(Mods.pokemon().spec(specs));
    }

    /**
     * Creates a stored Pokémon with a new Pokémon made from a spec.
     *
     * @param spec the spec
     */
    private StoredPokemon(UPokemonSpec<?, ?> spec) {
        this(spec.create());
    }

    /**
     * Creates a stored Pokémon with the shared {@link ConfigurablePokemon#instance()}.
     *
     * @param spec the configurable Pokémon
     */
    private StoredPokemon(ConfigurablePokemon spec) {
        this(spec.instance());
    }

    /**
     * Creates an empty stored Pokémon.
     *
     * @return the empty stored Pokémon
     */
    public static StoredPokemon empty() {
        return new StoredPokemon();
    }

    /**
     * Creates a stored Pokémon with a copy of the Pokémon as a tag.
     *
     * @param pokemon the Pokémon to store, or {@code null} for an empty stored Pokémon
     * @return the stored Pokémon
     */
    public static StoredPokemon of(UPokemon<?> pokemon) {
        return new StoredPokemon(pokemon);
    }

    /**
     * Creates a stored Pokémon from a tag.
     *
     * @param tag the tag of the Pokémon
     * @return the stored Pokémon
     */
    public static StoredPokemon of(CompoundTag tag) {
        return new StoredPokemon(tag);
    }

    /**
     * Creates a stored Pokémon with a new Pokémon made from a spec string.
     *
     * @param spec the spec string
     * @return the stored Pokémon
     */
    public static StoredPokemon of(String spec) {
        return new StoredPokemon(spec);
    }

    /**
     * Creates a stored Pokémon with a new Pokémon made from a spec.
     *
     * @param spec the spec
     * @return the stored Pokémon
     */
    public static StoredPokemon of(UPokemonSpec<?, ?> spec) {
        return new StoredPokemon(spec);
    }

    /**
     * Stores the shared {@link ConfigurablePokemon#instance()}, not a new Pokémon.
     */
    public static StoredPokemon of(ConfigurablePokemon spec) {
        return new StoredPokemon(spec);
    }

    /**
     * Stores a copy of the Pokémon as a tag. The tag does not follow later changes to the Pokémon. To save them, call
     * this method again.
     *
     * @param pokemon the Pokémon to store. Null clears this.
     */
    public void set(UPokemon<?> pokemon) {
        if (pokemon == null) {
            this.tag = null;
            this.pokemon = null;
        } else {
            this.tag = pokemon.toCompoundTag();
            this.pokemon = pokemon;
        }
    }

    /**
     * Removes the stored Pokémon and its tag.
     */
    public void clear() {
        this.set(null);
    }

    /**
     * @return the stored Pokémon, or null if this is empty. The first call reads it from the tag and caches it.
     */
    public UPokemon<?> actual() {
        if (this.isEmpty()) {
            return null;
        }

        if (this.pokemon == null) {
            this.pokemon = Mods.pokemon().pokemonFromTag(this.tag);
        }

        return this.pokemon;
    }

    /**
     * Checks if no Pokémon is stored.
     *
     * @return {@code true} if the tag is null or empty
     */
    public boolean isEmpty() {
        return this.tag == null || this.tag.isEmpty();
    }

    /**
     * @return the UUID of the stored Pokémon, or {@link UUIDUtils#ZEROED_UUID} if this is empty
     */
    public UUID uuid() {
        var actual = this.actual();
        return actual == null ? UUIDUtils.ZEROED_UUID : actual.uuid();
    }

    /**
     * Gets the hash code of the {@link #uuid()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.uuid().hashCode();
    }

    /**
     * Checks if the object is a stored Pokémon with the same {@link #uuid()}.
     *
     * @param obj the object to compare
     * @return {@code true} if the UUIDs are equal
     */
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

    /**
     * @return {@code StoredPokemon(<uuid>)}
     */
    @Override
    public String toString() {
        return "StoredPokemon(" + this.uuid() + ")";
    }

    /**
     * Reads and writes a {@link StoredPokemon} as the string form of its tag.
     */
    public static class TypeAdapter implements JsonSerializer<StoredPokemon>, JsonDeserializer<StoredPokemon> {

        /**
         * Reads a stored Pokémon from the string form of its tag. Logs an error if the tag cannot be parsed.
         *
         * @param json the JSON string
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return the stored Pokémon, which is empty if the string is null, empty or not a valid tag
         */
        @Override
        public StoredPokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull() || json.getAsString().isEmpty()) {
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

        /**
         * Writes the stored Pokémon as the string form of its tag.
         *
         * @param src the stored Pokémon
         * @param typeOfSrc the type to write
         * @param context the serialization context
         * @return the JSON string, or an empty string if there is no tag
         */
        @Override
        public JsonElement serialize(StoredPokemon src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.tag == null ? "" : src.tag.toString());
        }
    }

    /**
     * Creates an array in which each element is a new empty stored Pokémon.
     *
     * @param size the length of the array
     * @return the array
     */
    public static StoredPokemon[] emptyArrayOfSize(int size) {
        var pokemon = new StoredPokemon[size];
        for (var i = 0; i < pokemon.length; i++) {
            pokemon[i] = new StoredPokemon();
        }
        return pokemon;
    }
}
