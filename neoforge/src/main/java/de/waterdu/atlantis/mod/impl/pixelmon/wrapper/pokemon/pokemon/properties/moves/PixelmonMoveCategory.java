package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveCategory;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class PixelmonMoveCategory extends UMoveCategory<AttackCategory> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "move_category");

    public PixelmonMoveCategory(AttackCategory category) {
        super(TYPE, category);
    }

    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component displayName() {
        return Component.literal(this.dereference().getLocalizedName());
    }
}
