package de.waterdu.atlantis.mod.wrapper.pokemon.parsing;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.item.ItemStack;

public abstract class UPokemonSpec<T, P extends UPokemon<?>> extends ModWrapper<T> {

    protected UPokemonSpec(ModObjectKey type, T spec) {
        super(new ModObject(type, spec));
    }

    public abstract P create();

    public abstract ItemStack item();

    public abstract boolean matches(P pokemon);

    public abstract int countRequirements();

    public abstract String originalString();
}
