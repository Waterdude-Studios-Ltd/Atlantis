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

public class ConfigurablePokemon {

    private final String originalSpec;

    private UPokemonSpec<?, ?> spec = null;
    private UPokemon<?> instance = null;
    private ItemStack sprite = null;

    private int requirementCount = -1;

    private ConfigurablePokemon(String originalSpec) {
        this.originalSpec = originalSpec;
    }

    public static ConfigurablePokemon of(String... specs) {
        return new ConfigurablePokemon(TextUtils.concat(specs));
    }

    public static ConfigurablePokemon from(UPokemonSpec<?, ?> spec) {
        return new ConfigurablePokemon(spec.originalString());
    }

    public UPokemonSpec<?, ?> spec() {
        if (this.spec == null) {
            this.spec = Mods.pokemon().spec(this.originalSpec);
        }
        return this.spec;
    }

    public int requirementCount() {
        if (this.requirementCount == -1) {
            this.requirementCount = this.spec.countRequirements();
        }
        return this.requirementCount;
    }

    public UPokemon<?> instance() {
        if (this.instance == null) {
            this.instance = this.newInstance();
        }
        return this.instance;
    }

    public UPokemon<?> newInstance() {
        return this.spec.create();
    }

    public ItemStack sprite() {
        if (this.sprite == null) {
            this.sprite = this.spec.item();
        }
        return this.sprite.copy();
    }

    public StoredPokemon stored() {
        return StoredPokemon.of(this);
    }

    public boolean isValid() {
        return !this.originalSpec.isEmpty() && this.requirementCount() > 0;
    }

    public String originalSpec() {
        return this.originalSpec;
    }

    @Override
    public int hashCode() {
        return this.originalSpec.hashCode();
    }

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

    @Override
    public String toString() {
        return "ConfigurablePokemon(" + this.originalSpec + ")";
    }

    public static class TypeAdapter implements JsonSerializer<ConfigurablePokemon>, JsonDeserializer<ConfigurablePokemon> {

        @Override
        public ConfigurablePokemon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return ConfigurablePokemon.of(json.getAsString());
            } else {
                return PokemonSpecificationSerializer.deserialize(json.getAsJsonObject());
            }
        }

        @Override
        public JsonElement serialize(ConfigurablePokemon src, Type typeOfSrc, JsonSerializationContext context) {
            return PokemonSpecificationSerializer.serialize(src);
        }
    }
}