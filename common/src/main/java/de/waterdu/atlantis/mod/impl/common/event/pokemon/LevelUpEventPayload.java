package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class LevelUpEventPayload extends PokemonEventPayload implements Cancellable {

    private final int oldLevel;
    private int newLevel;
    private boolean cancelled = false;

    public LevelUpEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, int oldLevel, int newLevel) {
        super(source, player, pokemon);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_LEVEL_UP;
    }

    public int oldLevel() {
        return this.oldLevel;
    }

    public int newLevel() {
        return this.cancelled ? this.oldLevel : this.newLevel;
    }

    public void setNewLevel(int newLevel) {
        this.newLevel = newLevel;
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
