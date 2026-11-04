package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.battle;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattleParticipantType;

/**
 * Pixelmon has no participant type of its own. Thus, this class wraps the participant that it describes.
 */
public class PixelmonBattleParticipantType extends UBattleParticipantType<BattleParticipant> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_participant_type");

    /**
     * Creates a wrapper for the type of a Pixelmon battle participant.
     *
     * @param participant the participant whose type to wrap
     */
    public PixelmonBattleParticipantType(BattleParticipant participant) {
        super(TYPE, participant);
    }

    /**
     * Gets the Atlantis name of the type of the participant.
     *
     * @return {@link #PLAYER}, {@link #WILD}, {@link #TRAINER} or {@link #OTHER}
     */
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
