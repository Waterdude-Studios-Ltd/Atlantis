package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipantType;

/**
 * Wraps a Cobblemon battle actor type.
 */
public class CobblemonBattleParticipantType extends UBattleParticipantType<ActorType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle_participant_type");

    /**
     * Creates a wrapper for a Cobblemon actor type.
     *
     * @param type the actor type to wrap
     */
    public CobblemonBattleParticipantType(ActorType type) {
        super(TYPE, type);
    }

    /**
     * Gets the Atlantis name of the actor type. {@link ActorType#NPC} becomes {@link #TRAINER}.
     *
     * @return {@link #PLAYER}, {@link #WILD} or {@link #TRAINER}
     */
    @Override
    public String name() {
        return switch (this.dereference()) {
            case PLAYER -> PLAYER;
            case WILD -> WILD;
            case NPC -> TRAINER;
        };
    }
}
