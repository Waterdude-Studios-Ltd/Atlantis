package de.waterdu.atlantis.mod.wrapper.pokemon.entity;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public abstract class UPokemonEntity<T extends PathfinderMob> extends ModWrapper<T> {

    protected UPokemonEntity(ModObjectKey type, T entity) {
        super(new ModObject(type, entity));
    }

    public T entity() {
        return this.dereference();
    }

    public abstract UPokemon<?> pokemon();

    public abstract @Nullable UUID ownerUUID();

    public boolean isWild() {
        return this.ownerUUID() == null;
    }

    public abstract boolean isInBattle();

    public abstract @Nullable UBattle<?> battle();

    public abstract void recall();

    public void despawn() {
        this.entity().discard();
    }
}
