package de.waterdu.atlantis.mod.wrapper.pokemon.entity;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Wraps a Pokémon entity from the Pokémon mod.
 *
 * @param <T> the type of the wrapped entity
 */
public abstract class UPokemonEntity<T extends PathfinderMob> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a Pokémon entity.
     *
     * @param type the key of the mod object type
     * @param entity the entity to wrap
     */
    protected UPokemonEntity(ModObjectKey type, T entity) {
        super(new ModObject(type, entity));
    }

    /**
     * @return the wrapped entity
     */
    public T entity() {
        return this.dereference();
    }

    /**
     * @return the Pokémon that the entity shows
     */
    public abstract UPokemon<?> pokemon();

    /**
     * Gets the UUID of the owner of the Pokémon.
     *
     * @return the UUID, or {@code null} if the Pokémon is wild
     */
    public abstract @Nullable UUID ownerUUID();

    /**
     * Checks if the Pokémon is wild.
     *
     * @return {@code true} if the Pokémon has no owner
     */
    public boolean isWild() {
        return this.ownerUUID() == null;
    }

    /**
     * Checks if the Pokémon is in a battle.
     *
     * @return {@code true} if the Pokémon is in a battle
     */
    public abstract boolean isInBattle();

    /**
     * Gets the battle that the Pokémon is in.
     *
     * @return the battle, or {@code null} if the Pokémon is not in a battle
     */
    public abstract @Nullable UBattle<?> battle();

    /**
     * Sends the Pokémon back to its owner. Does nothing for a wild Pokémon.
     */
    public abstract void recall();

    /**
     * Removes the entity from the world.
     */
    public void despawn() {
        this.entity().discard();
    }
}
