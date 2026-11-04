package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps the type of a battle participant. The name is one of {@link #PLAYER}, {@link #WILD}, {@link #TRAINER} or
 * {@link #OTHER}.
 *
 * @param <T> the type of the wrapped participant type
 */
public abstract class UBattleParticipantType<T> extends ModWrapper<T> {

    public static final String PLAYER = "player";
    public static final String WILD = "wild";
    public static final String TRAINER = "trainer";
    public static final String OTHER = "other";

    /**
     * Creates a wrapper for the participant type.
     *
     * @param type the type of the wrapper
     * @param participantType the participant type to wrap
     */
    protected UBattleParticipantType(ModObjectKey type, T participantType) {
        super(new ModObject(type, participantType));
    }

    /**
     * @return the name of the participant type
     */
    public abstract String name();

    /**
     * Checks if this is the player type.
     *
     * @return {@code true} if the name is {@link #PLAYER}
     */
    public boolean isPlayer() {
        return this.name().equals(PLAYER);
    }

    /**
     * Checks if this is the wild type.
     *
     * @return {@code true} if the name is {@link #WILD}
     */
    public boolean isWild() {
        return this.name().equals(WILD);
    }

    /**
     * Checks if this is the trainer type.
     *
     * @return {@code true} if the name is {@link #TRAINER}
     */
    public boolean isTrainer() {
        return this.name().equals(TRAINER);
    }

    /**
     * Checks if this type has the same name as another type.
     *
     * @param participantType the other type
     * @return {@code true} if the names are equal
     */
    public boolean is(UBattleParticipantType<?> participantType) {
        return this.name().equals(participantType.name());
    }
}
