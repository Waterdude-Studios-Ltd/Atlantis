package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonForm;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonSpecies;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonEVs;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonIVs;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class CobblemonPokemon extends UPokemon<Pokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokemon");

    public CobblemonPokemon(Pokemon pokemon) {
        super(TYPE, pokemon);
    }

    @Override
    public UUID uuid() {
        return this.dereference().getUuid();
    }

    @Override
    public CobblemonSpecies species() {
        return new CobblemonSpecies(this.dereference().getSpecies());
    }

    @Override
    public CobblemonForm form() {
        return new CobblemonForm(this.dereference().getForm());
    }

    @Override
    public CobblemonIVs ivs() {
        return new CobblemonIVs(this.dereference().getIvs());
    }

    @Override
    public CobblemonEVs evs() {
        return new CobblemonEVs(this.dereference().getEvs());
    }

    @Override
    public boolean isShiny() {
        return this.dereference().getShiny();
    }

    @Override
    public CompoundTag toCompoundTag() {
        return this.dereference().saveToNBT(ServerUtils.registryAccess(), new CompoundTag());
    }

    @Override
    public JsonElement toJson() {
        return this.dereference().saveToJSON(ServerUtils.registryAccess(), new JsonObject());
    }
}
