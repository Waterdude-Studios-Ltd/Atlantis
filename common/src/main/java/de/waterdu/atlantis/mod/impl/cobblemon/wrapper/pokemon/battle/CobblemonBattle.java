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

public class CobblemonBattle extends UBattle<PokemonBattle> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle");

    public CobblemonBattle(PokemonBattle battle) {
        super(TYPE, battle);
    }

    @Override
    public String id() {
        return this.dereference().getBattleId().toString();
    }

    @Override
    public List<CobblemonBattleParticipant> participants() {
        var participants = new ArrayList<CobblemonBattleParticipant>();
        for (var actor : this.dereference().getActors()) {
            participants.add(new CobblemonBattleParticipant(actor));
        }
        return participants;
    }

    @Override
    public List<ServerPlayer> players() {
        return this.dereference().getPlayers();
    }

    @Override
    public boolean isPvP() {
        return this.dereference().isPvP();
    }

    @Override
    public int turn() {
        return this.dereference().getTurn();
    }

    @Override
    public boolean hasEnded() {
        return this.dereference().getEnded();
    }

    @Override
    public void end() {
        this.dereference().stop();
    }

    @Override
    public void forfeit(UBattleParticipant<?> loser) {
        var battle = this.dereference();
        var actor = (BattleActor) loser.dereference();
        var winner = Arrays.asList(battle.getSide1().getActors()).contains(actor) ? "p2" : "p1";
        battle.writeShowdownAction(">forcewin " + winner);
    }
}
