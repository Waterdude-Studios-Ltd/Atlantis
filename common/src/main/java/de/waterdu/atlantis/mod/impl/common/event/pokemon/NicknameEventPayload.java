package de.waterdu.atlantis.mod.impl.common.event.pokemon;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public class NicknameEventPayload extends PokemonEventPayload implements Cancellable {

    private @Nullable String nickname;
    private boolean cancelled = false;

    public NicknameEventPayload(Object source, ServerPlayer player, UPokemon<?> pokemon, @Nullable String nickname) {
        super(source, player, pokemon);
        this.nickname = nickname;
    }

    @Override
    public Key<?> type() {
        return UEvent.POKEMON_NICKNAME;
    }

    public @Nullable String nickname() {
        return this.nickname;
    }

    public void setNickname(@Nullable String nickname) {
        this.nickname = nickname;
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
