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

public class PixelmonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "species");

    public PixelmonSpecies(Species instance) {
        super(TYPE, instance);
    }

    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    @Override
    public String id() {
        return this.dereference().getName();
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    @Override
    public int dex() {
        return this.dereference().getDex();
    }

    @Override
    public PixelmonForm defaultForm() {
        return new PixelmonForm(this.dereference().getDefaultForm());
    }

    @Override
    public List<PixelmonForm> forms() {
        return this.dereference().getForms().stream().map(PixelmonForm::new).toList();
    }

    @Override
    public @Nullable PixelmonForm form(String name) {
        return this.dereference().hasForm(name) ? new PixelmonForm(this.dereference().getForm(name)) : null;
    }

    @Override
    public boolean isLegendary() {
        return this.dereference().isLegendary();
    }

    @Override
    public boolean isMythical() {
        return this.dereference().isMythical();
    }

    @Override
    public boolean isUltraBeast() {
        return this.dereference().isUltraBeast();
    }

    @Override
    public PixelmonPokemon create(int level) {
        var pokemon = PokemonFactory.create(this.dereference());
        pokemon.setLevel(level);
        return new PixelmonPokemon(pokemon);
    }
}
