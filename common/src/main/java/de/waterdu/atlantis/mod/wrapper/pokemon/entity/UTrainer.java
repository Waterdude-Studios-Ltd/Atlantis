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
 * {@code UEvent.PLAYER_INTERACT_ENTITY} to react to players right-clicking it.
 */
public abstract class UTrainer<T extends Entity> extends ModWrapper<T> {

    protected UTrainer(ModObjectKey type, T trainer) {
        super(new ModObject(type, trainer));
    }

    public T entity() {
        return this.dereference();
    }

    public abstract List<? extends UPokemon<?>> team();

    /**
     * Replaces the trainer's team with copies of the given Pokémon.
     */
    public abstract void setTeam(List<? extends UPokemon<?>> team);

    /**
     * Gives the trainer the skin of the player with this name. The skin is looked up asynchronously.
     */
    public abstract void setSkin(String playerName);

    public abstract void setImmovable(boolean immovable);

    public abstract void setInvulnerable(boolean invulnerable);

    public abstract void setNameVisible(boolean visible);

    public abstract @Nullable UBattle<?> battle();

    public boolean isInBattle() {
        return this.battle() != null;
    }
}
