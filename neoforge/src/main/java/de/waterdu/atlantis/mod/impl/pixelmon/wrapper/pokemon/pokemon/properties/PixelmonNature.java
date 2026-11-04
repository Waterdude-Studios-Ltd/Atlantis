package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats.PixelmonStat;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Wraps a Pixelmon nature.
 */
public class PixelmonNature extends UNature<Nature> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "nature");

    /**
     * Creates a wrapper for a Pixelmon nature.
     *
     * @param nature the nature to wrap
     */
    public PixelmonNature(Nature nature) {
        super(TYPE, nature);
    }

    /**
     * @return the enum name of the nature, in lower case
     */
    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the translated name of the nature
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    /**
     * Gets the stat that the nature increases.
     *
     * @return the stat, or {@code null} if the nature increases and decreases the same stat
     */
    @Override
    public @Nullable PixelmonStat increased() {
        var nature = this.dereference();
        return nature.getIncreasedStat() == nature.getDecreasedStat() ? null : new PixelmonStat(nature.getIncreasedStat());
    }

    /**
     * Gets the stat that the nature decreases.
     *
     * @return the stat, or {@code null} if the nature increases and decreases the same stat
     */
    @Override
    public @Nullable PixelmonStat decreased() {
        var nature = this.dereference();
        return nature.getIncreasedStat() == nature.getDecreasedStat() ? null : new PixelmonStat(nature.getDecreasedStat());
    }
}
