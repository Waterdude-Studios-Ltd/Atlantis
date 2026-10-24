package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

public abstract class UStat<T> extends ModWrapper<T> {

    public static final String HP = "hp";
    public static final String ATTACK = "attack";
    public static final String DEFENCE = "defence";
    public static final String SPECIAL_ATTACK = "special_attack";
    public static final String SPECIAL_DEFENCE = "special_defence";
    public static final String SPEED = "speed";
    public static final String ACCURACY = "accuracy";
    public static final String EVASION = "evasion";

    protected UStat(ModObjectKey type, T stat) {
        super(new ModObject(type, stat));
    }

    public abstract String name();

    public abstract Component displayName();

    public boolean isPermanent() {
        return !this.name().equals(ACCURACY) && !this.name().equals(EVASION);
    }

    public boolean is(UStat<?> stat) {
        return this.name().equals(stat.name());
    }
}
