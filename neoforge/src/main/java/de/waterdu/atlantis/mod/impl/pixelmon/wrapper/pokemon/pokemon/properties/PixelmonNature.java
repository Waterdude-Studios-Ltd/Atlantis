package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonStat;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public class PixelmonNature extends UNature<Nature> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "nature");

    public PixelmonNature(Nature nature) {
        super(TYPE, nature);
    }

    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    @Override
    public @Nullable PixelmonStat increased() {
        var nature = this.dereference();
        return nature.getIncreasedStat() == nature.getDecreasedStat() ? null : new PixelmonStat(nature.getIncreasedStat());
    }

    @Override
    public @Nullable PixelmonStat decreased() {
        var nature = this.dereference();
        return nature.getIncreasedStat() == nature.getDecreasedStat() ? null : new PixelmonStat(nature.getDecreasedStat());
    }
}
