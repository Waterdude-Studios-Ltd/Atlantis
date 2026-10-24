package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UBattleParticipantType<T> extends ModWrapper<T> {

    public static final String PLAYER = "player";
    public static final String WILD = "wild";
    public static final String TRAINER = "trainer";
    public static final String OTHER = "other";

    protected UBattleParticipantType(ModObjectKey type, T participantType) {
        super(new ModObject(type, participantType));
    }

    public abstract String name();

    public boolean isPlayer() {
        return this.name().equals(PLAYER);
    }

    public boolean isWild() {
        return this.name().equals(WILD);
    }

    public boolean isTrainer() {
        return this.name().equals(TRAINER);
    }

    public boolean is(UBattleParticipantType<?> participantType) {
        return this.name().equals(participantType.name());
    }
}
