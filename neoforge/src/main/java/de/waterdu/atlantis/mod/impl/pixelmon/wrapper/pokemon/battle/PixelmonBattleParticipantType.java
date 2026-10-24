package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipantType;

/**
 * Pixelmon has no participant type of its own, so this wraps the participant it describes.
 */
public class PixelmonBattleParticipantType extends UBattleParticipantType<BattleParticipant> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_participant_type");

    public PixelmonBattleParticipantType(BattleParticipant participant) {
        super(TYPE, participant);
    }

    @Override
    public String name() {
        var participant = this.dereference();
        if (participant.isPlayer()) {
            return PLAYER;
        } else if (participant.isWild()) {
            return WILD;
        } else if (participant.isTrainer()) {
            return TRAINER;
        }
        return OTHER;
    }
}
