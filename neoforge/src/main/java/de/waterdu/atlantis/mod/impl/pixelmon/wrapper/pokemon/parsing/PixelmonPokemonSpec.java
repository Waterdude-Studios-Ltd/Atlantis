package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.parsing;

import com.pixelmonmod.api.AbstractSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Wraps a Pixelmon Pokémon specification.
 */
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

    /**
     * Creates a wrapper for a Pixelmon Pokémon specification.
     *
     * @param spec the specification to wrap
     */
    public PixelmonPokemonSpec(PokemonSpecification spec) {
        super(TYPE, spec);
    }

    /**
     * Creates a new Pokémon from the specification.
     *
     * @return the new Pokémon
     */
    @Override
    public PixelmonPokemon create() {
        return new PixelmonPokemon(this.dereference().create());
    }

    /**
     * @return a Pixelmon photo item of a new Pokémon made from the specification
     */
    @Override
    public ItemStack item() {
        return SpriteItemHelper.getPhoto(this.dereference().create());
    }

    /**
     * Checks if the Pokémon matches the specification.
     *
     * @param pokemon the Pixelmon Pokémon to check
     * @return {@code true} if the Pokémon matches
     */
    @Override
    public boolean matches(UPokemon<?> pokemon) {
        return this.dereference().matches(PixelmonPokemon.unwrap(pokemon));
    }

    /**
     * Counts the requirements of the specification. Reads them from a private Pixelmon field with reflection.
     *
     * @return the number of requirements, or {@code -1} if the field cannot be read
     */
    @Override
    public int countRequirements() {
        try {
            return ((List<?>) REQUIREMENTS_FIELD.get(this.dereference())).size();
        } catch (IllegalAccessException __) {
            /* unreachable */
        }
        return -1;
    }

    /**
     * @return the string form of the specification
     */
    @Override
    public String originalString() {
        return this.dereference().toString();
    }
}
