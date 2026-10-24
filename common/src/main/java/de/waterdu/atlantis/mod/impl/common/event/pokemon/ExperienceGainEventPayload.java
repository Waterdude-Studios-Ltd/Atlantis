package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class ExperienceGainEventPayload extends PokemonEventPayload implements Cancellable {

    private final boolean fromBattle;
    private int experience;
    private boolean cancelled = false;

    public ExperienceGainEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int experience, boolean fromBattle) {
        super(source, player, pokemon);
        this.experience = experience;
        this.fromBattle = fromBattle;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_EXPERIENCE_GAIN;
    }

    public int experience() {
        return this.cancelled ? 0 : this.experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean fromBattle() {
        return this.fromBattle;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
