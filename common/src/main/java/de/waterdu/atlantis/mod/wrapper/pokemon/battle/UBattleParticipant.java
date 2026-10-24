package de.waterdu.atlantis.mod.wrapper.pokemon.battle;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class UBattleParticipant<T> extends ModWrapper<T> {

    protected UBattleParticipant(ModObjectKey type, T participant) {
        super(new ModObject(type, participant));
    }

    public abstract UBattleParticipantType<?> participantType();

    public abstract Component name();

    public abstract int side();

    public abstract @Nullable ServerPlayer player();

    public abstract @Nullable Entity entity();

    public abstract List<? extends UPokemon<?>> pokemon();

    public boolean isPlayer() {
        return this.participantType().isPlayer();
    }

    public boolean isWild() {
        return this.participantType().isWild();
    }

    public boolean isTrainer() {
        return this.participantType().isTrainer();
    }
}
