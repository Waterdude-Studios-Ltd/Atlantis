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

public class PixelmonBattle extends UBattle<BattleController> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle");

    public PixelmonBattle(BattleController battle) {
        super(TYPE, battle);
    }

    @Override
    public String id() {
        return String.valueOf(this.dereference().battleIndex());
    }

    @Override
    public List<PixelmonBattleParticipant> participants() {
        return this.dereference().participants().stream().map(PixelmonBattleParticipant::new).toList();
    }

    @Override
    public List<ServerPlayer> players() {
        return this.dereference().getPlayers().stream().map(participant -> participant.player).toList();
    }

    @Override
    public boolean isPvP() {
        return this.dereference().isPvP();
    }

    @Override
    public int turn() {
        return this.dereference().battleTurn();
    }

    @Override
    public boolean hasEnded() {
        return this.dereference().battleEnded();
    }

    @Override
    public void forfeit(UBattleParticipant<?> loser) {
        var side = ((BattleParticipant) loser.dereference()).team;
        var results = new HashMap<BattleParticipant, BattleResults>();
        for (var participant : this.dereference().participants()) {
            results.put(participant, participant.team == side ? BattleResults.DEFEAT : BattleResults.VICTORY);
        }
        this.dereference().endBattle(BattleEndCause.FORFEIT, results);
    }

    @Override
    public void end() {
        this.dereference().endBattle(BattleEndCause.COMMAND);
    }
}
