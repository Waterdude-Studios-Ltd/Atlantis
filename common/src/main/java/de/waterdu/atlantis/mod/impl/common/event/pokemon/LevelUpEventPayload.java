package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Holds the data of {@link UEvent#POKEMON_LEVEL_UP}, which fires when a Pokémon levels up.
 */
public class LevelUpEventPayload extends PokemonEventPayload implements Cancellable {

    private final int oldLevel;
    private int newLevel;
    private boolean cancelled = false;

    /**
     * Creates the payload for a Pokémon that levels up.
     *
     * @param source the event of the Pokémon mod that this payload wraps
     * @param player the owner of the Pokémon, or {@code null} if there is none
     * @param pokemon the Pokémon that levels up
     * @param oldLevel the level before the level up
     * @param newLevel the level after the level up
     */
    public LevelUpEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int oldLevel, int newLevel) {
        super(source, player, pokemon);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /**
     * @return {@link UEvent#POKEMON_LEVEL_UP}
     */
    @Override
    public Key<?> type() {
        return UEvent.POKEMON_LEVEL_UP;
    }

    /**
     * @return the level before the level up
     */
    public int oldLevel() {
        return this.oldLevel;
    }

    /**
     * @return the level after the level up, or the old level if the event is cancelled
     */
    public int newLevel() {
        return this.cancelled ? this.oldLevel : this.newLevel;
    }

    /**
     * Sets the level that the Pokémon gets.
     *
     * @param newLevel the new level
     */
    public void setNewLevel(int newLevel) {
        this.newLevel = newLevel;
    }

    /**
     * Cancels the event. The Pokémon then stays at its old level.
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
