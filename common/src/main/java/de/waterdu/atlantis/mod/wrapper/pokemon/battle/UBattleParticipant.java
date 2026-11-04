package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps a participant of a battle, such as a player, a wild Pokémon or an NPC trainer.
 *
 * @param <T> the type of the wrapped participant
 */
public abstract class UBattleParticipant<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for the participant.
     *
     * @param type the type of the wrapper
     * @param participant the participant to wrap
     */
    protected UBattleParticipant(ModObjectKey type, T participant) {
        super(new ModObject(type, participant));
    }

    /**
     * @return the type of the participant
     */
    public abstract UBattleParticipantType<?> participantType();

    /**
     * @return the name of the participant
     */
    public abstract Component name();

    /**
     * @return the side of the battle that the participant is on, {@code 0} or {@code 1}
     */
    public abstract int side();

    /**
     * @return the player, or {@code null} if the participant is not a player
     */
    public abstract @Nullable ServerPlayer player();

    /**
     * @return the entity of the participant, or {@code null} if it has none
     */
    public abstract @Nullable Entity entity();

    /**
     * @return the Pokémon of the participant in the battle
     */
    public abstract List<? extends UPokemon<?>> pokemon();

    /**
     * Checks if the participant is a player.
     *
     * @return {@code true} if the participant type is a player
     */
    public boolean isPlayer() {
        return this.participantType().isPlayer();
    }

    /**
     * Checks if the participant is a wild Pokémon.
     *
     * @return {@code true} if the participant type is wild
     */
    public boolean isWild() {
        return this.participantType().isWild();
    }

    /**
     * Checks if the participant is an NPC trainer.
     *
     * @return {@code true} if the participant type is a trainer
     */
    public boolean isTrainer() {
        return this.participantType().isTrainer();
    }
}
