package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wraps a Cobblemon battle.
 */
public class CobblemonBattle extends UBattle<PokemonBattle> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle");

    /**
     * Creates a wrapper for a Cobblemon battle.
     *
     * @param battle the battle to wrap
     */
    public CobblemonBattle(PokemonBattle battle) {
        super(TYPE, battle);
    }

    /**
     * Gets the battle ID of this battle as a string.
     *
     * @return the battle ID
     */
    @Override
    public String id() {
        return this.dereference().getBattleId().toString();
    }

    /**
     * Gets a new wrapper for each actor in the battle.
     *
     * @return the participants of the battle
     */
    @Override
    public List<CobblemonBattleParticipant> participants() {
        var participants = new ArrayList<CobblemonBattleParticipant>();
        for (var actor : this.dereference().getActors()) {
            participants.add(new CobblemonBattleParticipant(actor));
        }
        return participants;
    }

    /**
     * @return the players in the battle
     */
    @Override
    public List<ServerPlayer> players() {
        return this.dereference().getPlayers();
    }

    /**
     * Checks if the battle is between players only, as Cobblemon decides it.
     *
     * @return {@code true} if the battle is player versus player
     */
    @Override
    public boolean isPvP() {
        return this.dereference().isPvP();
    }

    /**
     * @return the current turn number of the battle
     */
    @Override
    public int turn() {
        return this.dereference().getTurn();
    }

    /**
     * Checks if the battle has ended.
     *
     * @return {@code true} if the battle has ended
     */
    @Override
    public boolean hasEnded() {
        return this.dereference().getEnded();
    }

    /**
     * Stops the battle at once.
     */
    @Override
    public void end() {
        this.dereference().stop();
    }

    /**
     * Sends a Showdown {@code >forcewin} action for the side that does not contain the given participant.
     *
     * @param loser the participant whose side loses
     */
    @Override
    public void forfeit(UBattleParticipant<?> loser) {
        var battle = this.dereference();
        var actor = (BattleActor) loser.dereference();
        var winner = Arrays.asList(battle.getSide1().getActors()).contains(actor) ? "p2" : "p1";
        battle.writeShowdownAction(">forcewin " + winner);
    }
}
