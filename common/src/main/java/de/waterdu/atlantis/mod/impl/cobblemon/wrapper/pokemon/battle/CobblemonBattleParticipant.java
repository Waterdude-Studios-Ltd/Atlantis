package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CobblemonBattleParticipant extends UBattleParticipant<BattleActor> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle_participant");

    public CobblemonBattleParticipant(BattleActor actor) {
        super(TYPE, actor);
    }

    @Override
    public CobblemonBattleParticipantType participantType() {
        return new CobblemonBattleParticipantType(this.dereference().getType());
    }

    @Override
    public Component name() {
        return this.dereference().getName();
    }

    @Override
    public int side() {
        var actor = this.dereference();
        return actor.getSide() == actor.getBattle().getSide1() ? 0 : 1;
    }

    @Override
    public @Nullable ServerPlayer player() {
        return this.dereference() instanceof PlayerBattleActor player ? player.getEntity() : null;
    }

    @Override
    public @Nullable Entity entity() {
        return this.dereference() instanceof EntityBackedBattleActor<?> actor ? actor.getEntity() : null;
    }

    @Override
    public List<CobblemonPokemon> pokemon() {
        var pokemon = new ArrayList<CobblemonPokemon>();
        for (var battlePokemon : this.dereference().getPokemonList()) {
            pokemon.add(new CobblemonPokemon(battlePokemon.getOriginalPokemon()));
        }
        return pokemon;
    }
}
