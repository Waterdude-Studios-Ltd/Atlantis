package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class USpecies<T> extends ModWrapper<T> {

    protected USpecies(ModObjectKey type, T species) {
        super(new ModObject(type, species));
    }

    public abstract String name();

    public abstract String id();

    public abstract Component displayName();

    public abstract int dex();

    public abstract UForm<?> defaultForm();

    public abstract List<? extends UForm<?>> forms();

    public abstract @Nullable UForm<?> form(String name);

    public abstract boolean isLegendary();

    public abstract boolean isMythical();

    public abstract boolean isUltraBeast();

    public abstract UPokemon<?> create(int level);

    public boolean is(USpecies<?> species) {
        return this.dex() == species.dex() && this.id().equalsIgnoreCase(species.id());
    }
}
