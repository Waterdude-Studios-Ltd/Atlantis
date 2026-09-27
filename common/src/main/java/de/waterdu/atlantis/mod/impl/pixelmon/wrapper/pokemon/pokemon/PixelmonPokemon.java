package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonEVs;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonForm;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonIVs;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonSpecies;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class PixelmonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokemon");

    public PixelmonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    @Override
    public UUID uuid() {
        return this.dereference().getUUID();
    }

    @Override
    public PixelmonSpecies species() {
        return new PixelmonSpecies(this.dereference().getSpecies());
    }

    @Override
    public PixelmonForm form() {
        return new PixelmonForm(this.dereference().getForm());
    }

    @Override
    public PixelmonIVs ivs() {
        return new PixelmonIVs(this.dereference().getIVs());
    }

    @Override
    public PixelmonEVs evs() {
        return new PixelmonEVs(this.dereference().getEVs());
    }

    @Override
    public boolean isShiny() {
        return this.dereference().isShiny();
    }

    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().writeToNBT(new CompoundTag(), ServerUtils.registryAccess());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.toCompoundTag().toString());
    }
}
