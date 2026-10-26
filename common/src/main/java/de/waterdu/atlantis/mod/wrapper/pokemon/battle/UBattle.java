package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class UBattle<T> extends ModWrapper<T> {

    protected UBattle(ModObjectKey type, T battle) {
        super(new ModObject(type, battle));
    }

    public abstract String id();

    public abstract List<? extends UBattleParticipant<?>> participants();

    public abstract List<ServerPlayer> players();

    public abstract boolean isPvP();

    public abstract int turn();

    public abstract boolean hasEnded();

    public abstract void end();

    /**
     * Ends the battle with the given participant's side losing.
     */
    public abstract void forfeit(UBattleParticipant<?> loser);

    public List<? extends UBattleParticipant<?>> side(int side) {
        var participants = new ArrayList<UBattleParticipant<?>>();
        for (var participant : this.participants()) {
            if (participant.side() == side) {
                participants.add(participant);
            }
        }
        return participants;
    }

    public @Nullable UBattleParticipant<?> participant(ServerPlayer player) {
        for (var participant : this.participants()) {
            if (player.equals(participant.player())) {
                return participant;
            }
        }
        return null;
    }

    public boolean contains(ServerPlayer player) {
        return this.participant(player) != null;
    }

    public boolean isWild() {
        return this.participants().stream().anyMatch(UBattleParticipant::isWild);
    }

    public boolean isTrainer() {
        return this.participants().stream().anyMatch(UBattleParticipant::isTrainer);
    }
}
