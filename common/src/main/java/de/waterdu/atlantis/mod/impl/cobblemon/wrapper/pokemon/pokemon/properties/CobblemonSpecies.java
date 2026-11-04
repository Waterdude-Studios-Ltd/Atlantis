package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.pokemon.Species;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Cobblemon species.
 */
public class CobblemonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "species");

    /**
     * Creates a wrapper for a Cobblemon species.
     *
     * @param instance the Cobblemon species to wrap
     */
    public CobblemonSpecies(Species instance) {
        super(TYPE, instance);
    }

    /**
     * Gets the translated name of this species as a plain string.
     *
     * @return the translated name of the species
     */
    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    /**
     * Gets the path of the Cobblemon resource identifier of this species.
     *
     * @return the identifier path, without the namespace
     */
    @Override
    public String id() {
        return this.dereference().getResourceIdentifier().getPath();
    }

    /**
     * @return the translated name of the species as a component
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    /**
     * @return the national Pokédex number of the species
     */
    @Override
    public int dex() {
        return this.dereference().getNationalPokedexNumber();
    }

    /**
     * Gets the standard form of this species.
     *
     * @return a new wrapper for the standard form
     */
    @Override
    public CobblemonForm defaultForm() {
        return new CobblemonForm(this.dereference().getStandardForm());
    }

    /**
     * Gets all forms of this species. The standard form is always first, and it occurs only once.
     *
     * @return a new list of the forms
     */
    @Override
    public List<CobblemonForm> forms() {
        var forms = new ArrayList<CobblemonForm>();
        forms.add(this.defaultForm());
        for (var form : this.dereference().getForms()) {
            if (!form.equals(this.dereference().getStandardForm())) {
                forms.add(new CobblemonForm(form));
            }
        }
        return forms;
    }

    /**
     * Finds the form of this species with the given name. The name comparison ignores case.
     *
     * @param name the name of the form
     * @return the form, or {@code null} if this species has no form with that name
     */
    @Override
    public @Nullable CobblemonForm form(String name) {
        for (var form : this.forms()) {
            if (form.name().equalsIgnoreCase(name)) {
                return form;
            }
        }
        return null;
    }

    /**
     * Checks if this species has the Cobblemon legendary label.
     *
     * @return {@code true} if the species is legendary
     */
    @Override
    public boolean isLegendary() {
        return this.hasLabel(CobblemonPokemonLabels.LEGENDARY);
    }

    /**
     * Checks if this species has the Cobblemon mythical label.
     *
     * @return {@code true} if the species is mythical
     */
    @Override
    public boolean isMythical() {
        return this.hasLabel(CobblemonPokemonLabels.MYTHICAL);
    }

    /**
     * Checks if this species has the Cobblemon Ultra Beast label.
     *
     * @return {@code true} if the species is an Ultra Beast
     */
    @Override
    public boolean isUltraBeast() {
        return this.hasLabel(CobblemonPokemonLabels.ULTRA_BEAST);
    }

    /**
     * Creates a new Cobblemon Pokémon of this species.
     *
     * @param level the level of the new Pokémon
     * @return a wrapper for the new Pokémon
     */
    @Override
    public CobblemonPokemon create(int level) {
        return new CobblemonPokemon(this.dereference().create(level));
    }

    /**
     * Checks if this species has the given label. The label comparison ignores case.
     *
     * @param label the label to look for
     * @return {@code true} if the species has the label
     */
    private boolean hasLabel(String label) {
        return this.dereference().getLabels().stream().anyMatch(it -> it.equalsIgnoreCase(label));
    }
}
