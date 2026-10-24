package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class UNature<T> extends ModWrapper<T> {

    protected UNature(ModObjectKey type, T nature) {
        super(new ModObject(type, nature));
    }

    public abstract String name();

    public abstract Component displayName();

    public abstract @Nullable UStat<?> increased();

    public abstract @Nullable UStat<?> decreased();

    public boolean isNeutral() {
        return this.increased() == null;
    }

    public boolean is(UNature<?> nature) {
        return this.name().equals(nature.name());
    }
}
