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

/**
 * Wraps a Cobblemon battle actor.
 */
public class CobblemonBattleParticipant extends UBattleParticipant<BattleActor> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle_participant");

    /**
     * Creates a wrapper for a Cobblemon battle actor.
     *
     * @param actor the actor to wrap
     */
    public CobblemonBattleParticipant(BattleActor actor) {
        super(TYPE, actor);
    }

    /**
     * Gets the type of the actor, such as player, wild or NPC.
     *
     * @return the participant type
     */
    @Override
    public CobblemonBattleParticipantType participantType() {
        return new CobblemonBattleParticipantType(this.dereference().getType());
    }

    /**
     * @return the display name of the actor
     */
    @Override
    public Component name() {
        return this.dereference().getName();
    }

    /**
     * Gets the side of the battle that the actor is on.
     *
     * @return {@code 0} for the first side, or {@code 1} for the second side
     */
    @Override
    public int side() {
        var actor = this.dereference();
        return actor.getSide() == actor.getBattle().getSide1() ? 0 : 1;
    }

    /**
     * Gets the player of the actor.
     *
     * @return the player, or {@code null} if the actor is not a player actor or the player is not online
     */
    @Override
    public @Nullable ServerPlayer player() {
        return this.dereference() instanceof PlayerBattleActor player ? player.getEntity() : null;
    }

    /**
     * Gets the entity of the actor.
     *
     * @return the entity, or {@code null} if the actor is not backed by an entity
     */
    @Override
    public @Nullable Entity entity() {
        return this.dereference() instanceof EntityBackedBattleActor<?> actor ? actor.getEntity() : null;
    }

    /**
     * Gets the original Pokémon behind each battle Pokémon of the actor.
     *
     * @return the Pokémon of the actor
     */
    @Override
    public List<CobblemonPokemon> pokemon() {
        var pokemon = new ArrayList<CobblemonPokemon>();
        for (var battlePokemon : this.dereference().getPokemonList()) {
            pokemon.add(new CobblemonPokemon(battlePokemon.getOriginalPokemon()));
        }
        return pokemon;
    }
}
