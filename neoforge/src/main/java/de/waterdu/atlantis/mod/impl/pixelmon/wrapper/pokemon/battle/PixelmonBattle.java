package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.BattleEndCause;
import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;

/**
 * Wraps a Pixelmon battle controller.
 */
public class PixelmonBattle extends UBattle<BattleController> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle");

    /**
     * Creates a wrapper for a Pixelmon battle controller.
     *
     * @param battle the battle controller to wrap
     */
    public PixelmonBattle(BattleController battle) {
        super(TYPE, battle);
    }

    /**
     * Gets the battle index of this battle as a string.
     *
     * @return the battle index
     */
    @Override
    public String id() {
        return String.valueOf(this.dereference().battleIndex());
    }

    /**
     * Gets a new wrapper for each participant in the battle.
     *
     * @return the participants of the battle
     */
    @Override
    public List<PixelmonBattleParticipant> participants() {
        return this.dereference().participants().stream().map(PixelmonBattleParticipant::new).toList();
    }

    /**
     * @return the players in the battle
     */
    @Override
    public List<ServerPlayer> players() {
        return this.dereference().getPlayers().stream().map(participant -> participant.player).toList();
    }

    /**
     * Checks if the battle is player versus player, as Pixelmon decides it.
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
        return this.dereference().battleTurn();
    }

    /**
     * Checks if the battle has ended.
     *
     * @return {@code true} if the battle has ended
     */
    @Override
    public boolean hasEnded() {
        return this.dereference().battleEnded();
    }

    /**
     * Ends the battle with a forfeit. Each participant on the side of the loser gets a defeat, and every other
     * participant gets a victory.
     *
     * @param loser the participant whose side loses
     */
    @Override
    public void forfeit(UBattleParticipant<?> loser) {
        var side = ((BattleParticipant) loser.dereference()).team;
        var results = new HashMap<BattleParticipant, BattleResults>();
        for (var participant : this.dereference().participants()) {
            results.put(participant, participant.team == side ? BattleResults.DEFEAT : BattleResults.VICTORY);
        }
        this.dereference().endBattle(BattleEndCause.FORFEIT, results);
    }

    /**
     * Ends the battle with the command end cause.
     */
    @Override
    public void end() {
        this.dereference().endBattle(BattleEndCause.COMMAND);
    }
}
