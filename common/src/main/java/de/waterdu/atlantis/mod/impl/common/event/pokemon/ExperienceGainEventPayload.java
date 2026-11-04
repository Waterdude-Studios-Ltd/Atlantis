package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Holds the data of {@link UEvent#POKEMON_EXPERIENCE_GAIN}, which fires before a Pokémon gains experience.
 */
public class ExperienceGainEventPayload extends PokemonEventPayload implements Cancellable {

    private final boolean fromBattle;
    private int experience;
    private boolean cancelled = false;

    /**
     * Creates the payload for a Pokémon that gains experience.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon that gains experience
     * @param experience the experience that the Pokémon gains
     * @param fromBattle {@code true} if the experience comes from a battle
     */
    public ExperienceGainEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int experience, boolean fromBattle) {
        super(source, player, pokemon);
        this.experience = experience;
        this.fromBattle = fromBattle;
    }

    /**
     * @return {@link UEvent#POKEMON_EXPERIENCE_GAIN}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_EXPERIENCE_GAIN;
    }

    /**
     * @return the experience that the Pokémon gains, or {@code 0} if the event is cancelled
     */
    public int experience() {
        return this.cancelled ? 0 : this.experience;
    }

    /**
     * Sets the experience that the Pokémon gains.
     *
     * @param experience the new experience
     */
    public void setExperience(int experience) {
        this.experience = experience;
    }

    /**
     * @return {@code true} if the experience comes from a battle
     */
    public boolean fromBattle() {
        return this.fromBattle;
    }

    /**
     * Cancels the event. The Pokémon then gains no experience.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @return {@code true} if the event is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
