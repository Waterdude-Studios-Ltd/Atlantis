package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.battle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipantType;

public class CobblemonBattleParticipantType extends UBattleParticipantType<ActorType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "battle_participant_type");

    public CobblemonBattleParticipantType(ActorType type) {
        super(TYPE, type);
    }

    @Override
    public String name() {
        return switch (this.dereference()) {
            case PLAYER -> PLAYER;
            case WILD -> WILD;
            case NPC -> TRAINER;
        };
    }
}
