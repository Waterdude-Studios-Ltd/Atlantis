package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.parsing;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import net.minecraft.world.item.ItemStack;

public class CobblemonPokemonSpec extends UPokemonSpec<PokemonProperties, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "spec");

    public CobblemonPokemonSpec(PokemonProperties spec) {
        super(TYPE, spec);
    }

    @Override
    public CobblemonPokemon create() {
        return new CobblemonPokemon(this.dereference().create());
    }

    @Override
    public ItemStack item() {
        return PokemonItem.from(this.dereference());
    }

    @Override
    public boolean matches(CobblemonPokemon pokemon) {
        return this.dereference().matches(pokemon.dereference());
    }

    @Override
    public int countRequirements() {
        return this.dereference().getAspects().size();
    }

    @Override
    public String originalString() {
        return this.dereference().getOriginalString();
    }
}
