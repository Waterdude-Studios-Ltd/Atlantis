package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class PixelmonElementalType extends UElementalType<Holder<Type>> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "elemental_type");

    public PixelmonElementalType(Holder<Type> elementalType) {
        super(TYPE, elementalType);
    }

    @Override
    public String name() {
        return this.dereference().unwrapKey()
                .map(key -> key.location().getPath())
                .orElseGet(() -> this.displayName().getString())
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public Component displayName() {
        return this.dereference().value().name();
    }
}
