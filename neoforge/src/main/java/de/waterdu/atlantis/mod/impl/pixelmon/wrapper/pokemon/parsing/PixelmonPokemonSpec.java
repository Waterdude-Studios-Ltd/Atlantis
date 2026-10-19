package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.parsing;

import com.pixelmonmod.api.AbstractSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

public class PixelmonPokemonSpec extends UPokemonSpec<PokemonSpecification, PixelmonPokemon> {

    private static final Field REQUIREMENTS_FIELD;
    static {
        try {
            REQUIREMENTS_FIELD = AbstractSpecification.class.getDeclaredField("requirements");
            REQUIREMENTS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "spec");

    public PixelmonPokemonSpec(PokemonSpecification spec) {
        super(TYPE, spec);
    }

    @Override
    public PixelmonPokemon create() {
        return new PixelmonPokemon(this.dereference().create());
    }

    @Override
    public ItemStack item() {
        return SpriteItemHelper.getPhoto(this.dereference().create());
    }

    @Override
    public boolean matches(PixelmonPokemon pokemon) {
        return this.dereference().matches(pokemon.dereference());
    }

    @Override
    public int countRequirements() {
        try {
            return ((List<?>) REQUIREMENTS_FIELD.get(this.dereference())).size();
        } catch (IllegalAccessException __) {
            /* unreachable */
        }
        return -1;
    }

    @Override
    public String originalString() {
        return this.dereference().toString();
    }
}
