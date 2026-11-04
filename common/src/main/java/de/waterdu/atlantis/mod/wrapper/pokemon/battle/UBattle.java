package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Pokémon battle of the mod.
 *
 * @param <T> the type of the wrapped battle
 */
public abstract class UBattle<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for the battle.
     *
     * @param type the type of the wrapper
     * @param battle the battle to wrap
     */
    protected UBattle(ModObjectKey type, T battle) {
        super(new ModObject(type, battle));
    }

    /**
     * @return the id of the battle
     */
    public abstract String id();

    /**
     * @return the participants on both sides of the battle
     */
    public abstract List<? extends UBattleParticipant<?>> participants();

    /**
     * @return the players in the battle
     */
    public abstract List<ServerPlayer> players();

    /**
     * Checks if the battle is between players.
     *
     * @return {@code true} if the battle is player versus player
     */
    public abstract boolean isPvP();

    /**
     * @return the number of the current turn
     */
    public abstract int turn();

    /**
     * Checks if the battle has ended.
     *
     * @return {@code true} if the battle has ended
     */
    public abstract boolean hasEnded();

    /**
     * Ends the battle.
     */
    public abstract void end();

    /**
     * Ends the battle with the given participant's side losing.
     */
    public abstract void forfeit(UBattleParticipant<?> loser);

    /**
     * @param side the side of the battle, {@code 0} or {@code 1}
     */
    public List<? extends UBattleParticipant<?>> side(int side) {
        var participants = new ArrayList<UBattleParticipant<?>>();
        for (var participant : this.participants()) {
            if (participant.side() == side) {
                participants.add(participant);
            }
        }
        return participants;
    }

    /**
     * Finds the participant of the player.
     *
     * @param player the player
     * @return the participant, or {@code null} if the player is not in the battle
     */
    public @Nullable UBattleParticipant<?> participant(ServerPlayer player) {
        for (var participant : this.participants()) {
            if (player.equals(participant.player())) {
                return participant;
            }
        }
        return null;
    }

    /**
     * Checks if the player is in the battle.
     *
     * @param player the player
     * @return {@code true} if the player is a participant
     */
    public boolean contains(ServerPlayer player) {
        return this.participant(player) != null;
    }

    /**
     * Checks if a wild Pokémon is in the battle.
     *
     * @return {@code true} if a participant is wild
     */
    public boolean isWild() {
        return this.participants().stream().anyMatch(UBattleParticipant::isWild);
    }

    /**
     * Checks if an NPC trainer is in the battle.
     *
     * @return {@code true} if a participant is a trainer
     */
    public boolean isTrainer() {
        return this.participants().stream().anyMatch(UBattleParticipant::isTrainer);
    }
}
