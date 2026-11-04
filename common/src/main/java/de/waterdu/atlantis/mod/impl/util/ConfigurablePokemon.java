package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Holds a Pokémon spec string for configs. The spec, the Pokémon and the sprite are parsed on first use and cached.
 */
public class ConfigurablePokemon {

    private final String originalSpec;

    private UPokemonSpec<?, ?> spec = null;
    private UPokemon<?> instance = null;
    private ItemStack sprite = null;

    private int requirementCount = -1;

    /**
     * Creates a configurable Pokémon from a spec string.
     *
     * @param originalSpec the spec string
     */
    private ConfigurablePokemon(String originalSpec) {
        this.originalSpec = originalSpec;
    }

    /**
     * @param specs the parts of the spec. They are joined with spaces.
     */
    public static ConfigurablePokemon of(String... specs) {
        return new ConfigurablePokemon(TextUtils.concat(specs));
    }

    /**
     * Creates a configurable Pokémon from the original string of a spec.
     *
     * @param spec the spec
     * @return the configurable Pokémon
     */
    public static ConfigurablePokemon from(UPokemonSpec<?, ?> spec) {
        return new ConfigurablePokemon(spec.originalString());
    }

    /**
     * Gets the spec. The first call parses it with the Pokémon delegate and caches it.
     *
     * @return the parsed spec
     */
    public UPokemonSpec<?, ?> spec() {
        if (this.spec == null) {
            this.spec = Mods.pokemon().spec(this.originalSpec);
        }
        return this.spec;
    }

    /**
     * Gets the number of requirements in the spec. The first call counts them and caches the result.
     *
     * @return the number of requirements
     */
    public int requirementCount() {
        if (this.requirementCount == -1) {
            this.requirementCount = this.spec().countRequirements();
        }
        return this.requirementCount;
    }

    /**
     * Creates the Pokémon on the first call and returns the same object after that. Thus, do not change it. For a
     * Pokémon that you can change, use {@link #newInstance()}.
     */
    public UPokemon<?> instance() {
        if (this.instance == null) {
            this.instance = this.newInstance();
        }
        return this.instance;
    }

    /**
     * Creates a new Pokémon from the spec on each call.
     *
     * @return the new Pokémon
     */
    public UPokemon<?> newInstance() {
        return this.spec().create();
    }

    /**
     * Gets a display item for the spec. The first call creates the item and caches it.
     *
     * @return a copy of the cached item
     */
    public ItemStack sprite() {
        if (this.sprite == null) {
            this.sprite = this.spec().item();
        }
        return this.sprite.copy();
    }

    /**
     * Creates a {@link StoredPokemon} that stores the shared {@link #instance()}.
     *
     * @return the stored Pokémon
     */
    public StoredPokemon stored() {
        return StoredPokemon.of(this);
    }

    /**
     * Checks if the spec string is not empty and has at least one requirement.
     *
     * @return {@code true} if the spec is valid
     */
    public boolean isValid() {
        return !this.originalSpec.isEmpty() && this.requirementCount() > 0;
    }

    /**
     * @return the spec string that this was created from
     */
    public String originalSpec() {
        return this.originalSpec;
    }

    /**
     * Gets the hash code of the spec string in lower case.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.originalSpec.toLowerCase(Locale.ROOT).hashCode();
    }

    /**
     * Checks if the object is a configurable Pokémon with the same spec string, ignoring case.
     *
     * @param obj the object to compare
     * @return {@code true} if the spec strings are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigurablePokemon other)) {
            return false;
        }
        return this.originalSpec.equalsIgnoreCase(other.originalSpec);
    }

    /**
     * @return {@code ConfigurablePokemon(<spec>)}
     */
    @Override
    public String toString() {
        return "ConfigurablePokemon(" + this.originalSpec + ")";
    }

    /**
     * Reads and writes a {@link ConfigurablePokemon} as JSON.
     */
    public static class TypeAdapter implements JsonSerializer<ConfigurablePokemon>, JsonDeserializer<ConfigurablePokemon> {

        /**
         * Reads a configurable Pokémon from a spec string, or from an object with one key for each spec part.
         *
         * @param json the JSON string or object
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return the configurable Pokémon
         */
        @Override
        public ConfigurablePokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return ConfigurablePokemon.of(json.getAsString());
            } else {
                return PokemonSpecificationSerializer.deserialize(json.getAsJsonObject());
            }
        }

        /**
         * Writes the configurable Pokémon as an object with {@link PokemonSpecificationSerializer}.
         *
         * @param src the configurable Pokémon
         * @param typeOfSrc the type to write
         * @param context the serialization context
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(ConfigurablePokemon src, Type typeOfSrc, JsonSerializationContext context) {
            return PokemonSpecificationSerializer.serialize(src);
        }
    }
}