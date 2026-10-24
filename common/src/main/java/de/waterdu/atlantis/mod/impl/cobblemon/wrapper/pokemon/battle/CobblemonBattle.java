package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
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
}
