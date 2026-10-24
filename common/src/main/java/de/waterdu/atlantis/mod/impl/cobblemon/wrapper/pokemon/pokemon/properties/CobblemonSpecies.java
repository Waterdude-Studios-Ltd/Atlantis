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

public class CobblemonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "species");

    public CobblemonSpecies(Species instance) {
        super(TYPE, instance);
    }

    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    @Override
    public String id() {
        return this.dereference().getResourceIdentifier().getPath();
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    @Override
    public int dex() {
        return this.dereference().getNationalPokedexNumber();
    }

    @Override
    public CobblemonForm defaultForm() {
        return new CobblemonForm(this.dereference().getStandardForm());
    }

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

    @Override
    public @Nullable CobblemonForm form(String name) {
        for (var form : this.forms()) {
            if (form.name().equalsIgnoreCase(name)) {
                return form;
            }
        }
        return null;
    }

    @Override
    public boolean isLegendary() {
        return this.hasLabel(CobblemonPokemonLabels.LEGENDARY);
    }

    @Override
    public boolean isMythical() {
        return this.hasLabel(CobblemonPokemonLabels.MYTHICAL);
    }

    @Override
    public boolean isUltraBeast() {
        return this.hasLabel(CobblemonPokemonLabels.ULTRA_BEAST);
    }

    @Override
    public CobblemonPokemon create(int level) {
        return new CobblemonPokemon(this.dereference().create(level));
    }

    private boolean hasLabel(String label) {
        return this.dereference().getLabels().stream().anyMatch(it -> it.equalsIgnoreCase(label));
    }
}
