package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.battle.UBattle;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class MegaEvolveEventPayload extends PokemonEventPayload {

    private final @Nullable UBattle<?> battle;

    public MegaEvolveEventPayload(Object source, @Nullable ServerPlayer player, UPokemon<?> pokemon, @Nullable UBattle<?> battle) {
        super(source, player, pokemon);
        this.battle = battle;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_MEGA_EVOLVE;
    }

    public @Nullable UBattle<?> battle() {
        return this.battle;
    }
}
