package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps a Pixelmon species.
 */
public class PixelmonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "species");

    /**
     * Creates a wrapper for a Pixelmon species.
     *
     * @param instance the species to wrap
     */
    public PixelmonSpecies(Species instance) {
        super(TYPE, instance);
    }

    /**
     * @return the translated name of the species, as plain text
     */
    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    /**
     * @return the Pixelmon name of the species
     */
    @Override
    public String id() {
        return this.dereference().getName();
    }

    /**
     * @return the translated name of the species
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    /**
     * @return the National Pokédex number of the species
     */
    @Override
    public int dex() {
        return this.dereference().getDex();
    }

    /**
     * @return a new wrapper for the default form of the species
     */
    @Override
    public PixelmonForm defaultForm() {
        return new PixelmonForm(this.dereference().getDefaultForm());
    }

    /**
     * @return new wrappers for all forms of the species
     */
    @Override
    public List<PixelmonForm> forms() {
        return this.dereference().getForms().stream().map(PixelmonForm::new).toList();
    }

    /**
     * Gets a form of the species by name.
     *
     * @param name the name of the form
     * @return the form, or {@code null} if the species has no form with that name
     */
    @Override
    public @Nullable PixelmonForm form(String name) {
        return this.dereference().hasForm(name) ? new PixelmonForm(this.dereference().getForm(name)) : null;
    }

    /**
     * Checks if the species is legendary.
     *
     * @return {@code true} if the species is legendary
     */
    @Override
    public boolean isLegendary() {
        return this.dereference().isLegendary();
    }

    /**
     * Checks if the species is mythical.
     *
     * @return {@code true} if the species is mythical
     */
    @Override
    public boolean isMythical() {
        return this.dereference().isMythical();
    }

    /**
     * Checks if the species is an Ultra Beast.
     *
     * @return {@code true} if the species is an Ultra Beast
     */
    @Override
    public boolean isUltraBeast() {
        return this.dereference().isUltraBeast();
    }

    /**
     * Creates a new Pokémon of the species at the level.
     *
     * @param level the level of the new Pokémon
     * @return the new Pokémon
     */
    @Override
    public PixelmonPokemon create(int level) {
        var pokemon = PokemonFactory.create(this.dereference());
        pokemon.setLevel(level);
        return new PixelmonPokemon(pokemon);
    }
}
