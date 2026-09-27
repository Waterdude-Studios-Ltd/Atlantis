package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon;

import com.google.gson.JsonElement;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UIVs;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public abstract class UPokemon<T> extends ModWrapper<T> {

    protected UPokemon(ModObjectKey type, T pokemon) {
        super(new ModObject(type, pokemon));
    }

    public abstract UUID uuid();

    public abstract USpecies<?> species();

    public abstract UForm<?> form();

    public abstract UIVs<?, ?> ivs();

    public abstract UEVs<?, ?> evs();

    public abstract boolean isShiny();

    public abstract CompoundTag toCompoundTag();

    public abstract JsonElement toJson();
}
