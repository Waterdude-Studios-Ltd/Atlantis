package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Pixelmon battle participant.
 */
public class PixelmonBattleParticipant extends UBattleParticipant<BattleParticipant> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_participant");

    /**
     * Creates a wrapper for a Pixelmon battle participant.
     *
     * @param participant the participant to wrap
     */
    public PixelmonBattleParticipant(BattleParticipant participant) {
        super(TYPE, participant);
    }

    /**
     * @return a new wrapper for the type of this participant
     */
    @Override
    public PixelmonBattleParticipantType participantType() {
        return new PixelmonBattleParticipantType(this.dereference());
    }

    /**
     * @return the name of the participant
     */
    @Override
    public Component name() {
        return this.dereference().getName();
    }

    /**
     * @return the team number of the participant
     */
    @Override
    public int side() {
        return this.dereference().team;
    }

    /**
     * Gets the player of the participant.
     *
     * @return the player, or {@code null} if the participant is not a player
     */
    @Override
    public @Nullable ServerPlayer player() {
        return this.dereference() instanceof PlayerParticipant player ? player.player : null;
    }

    /**
     * Gets the entity of the participant.
     *
     * @return the entity, or {@code null} if the participant has no entity
     */
    @Override
    public @Nullable Entity entity() {
        return this.dereference().getEntity();
    }

    /**
     * Gets a new wrapper for each Pokémon of the participant.
     *
     * @return the Pokémon, or an empty list if the participant has none
     */
    @Override
    public List<PixelmonPokemon> pokemon() {
        var pokemon = new ArrayList<PixelmonPokemon>();
        if (this.dereference().allPokemon != null) {
            for (var wrapper : this.dereference().allPokemon) {
                pokemon.add(new PixelmonPokemon(wrapper));
            }
        }
        return pokemon;
    }
}
