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

public class PixelmonBattleParticipant extends UBattleParticipant<BattleParticipant> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "battle_participant");

    public PixelmonBattleParticipant(BattleParticipant participant) {
        super(TYPE, participant);
    }

    @Override
    public PixelmonBattleParticipantType participantType() {
        return new PixelmonBattleParticipantType(this.dereference());
    }

    @Override
    public Component name() {
        return this.dereference().getName();
    }

    @Override
    public int side() {
        return this.dereference().team;
    }

    @Override
    public @Nullable ServerPlayer player() {
        return this.dereference() instanceof PlayerParticipant player ? player.player : null;
    }

    @Override
    public @Nullable Entity entity() {
        return this.dereference().getEntity();
    }

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
