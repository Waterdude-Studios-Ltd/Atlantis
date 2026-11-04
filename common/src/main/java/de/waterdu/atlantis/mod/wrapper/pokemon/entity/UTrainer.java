package de.waterdu.atlantis.mod.wrapper.pokemon.entity;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An NPC trainer from the Pokémon mod, spawned with {@code PokemonDelegate#spawnTrainer}. Use
 * {@code UEvent.PLAYER_INTERACT_ENTITY} to react when a player right-clicks it.
 */
public abstract class UTrainer<T extends Entity> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a trainer entity.
     *
     * @param type the key of the mod object type
     * @param trainer the trainer entity to wrap
     */
    protected UTrainer(ModObjectKey type, T trainer) {
        super(new ModObject(type, trainer));
    }

    /**
     * @return the wrapped trainer entity
     */
    public T entity() {
        return this.dereference();
    }

    /**
     * @return the Pokémon in the trainer's team
     */
    public abstract List<? extends UPokemon<?>> team();

    /**
     * Replaces the trainer's team with copies of the given Pokémon.
     */
    public abstract void setTeam(List<? extends UPokemon<?>> team);

    /**
     * Gives the trainer the skin of the player with this name. The skin lookup is asynchronous.
     */
    public abstract void setSkin(String playerName);

    /**
     * Sets if the trainer can move.
     *
     * @param immovable {@code true} to stop the trainer from moving
     */
    public abstract void setImmovable(boolean immovable);

    /**
     * Sets if the trainer can take damage.
     *
     * @param invulnerable {@code true} to stop the trainer from taking damage
     */
    public abstract void setInvulnerable(boolean invulnerable);

    /**
     * Sets if the name of the trainer shows above it.
     *
     * @param visible {@code true} to show the name
     */
    public abstract void setNameVisible(boolean visible);

    /**
     * Gets the battle that the trainer is in.
     *
     * @return the battle, or {@code null} if the trainer is not in a battle
     */
    public abstract @Nullable UBattle<?> battle();

    /**
     * Checks if the trainer is in a battle.
     *
     * @return {@code true} if {@link #battle()} is not {@code null}
     */
    public boolean isInBattle() {
        return this.battle() != null;
    }
}
