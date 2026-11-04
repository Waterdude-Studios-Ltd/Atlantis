package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.UniquelyNamed;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.Optional;

/**
 * Marks data that belongs to one player. The UUID of the instance is the UUID of the player.
 */
public interface PlayerData extends NamedData, UniquelyNamed {

    /**
     * Called when the player joins, after the data loads. For new data, called before the first save.
     * Can run off the server thread, depending on the storage.
     */
    void onLogin(ServerPlayer player);

    /**
     * Called when the player leaves. Data that waits for the autosave is written after this call.
     * Can run off the server thread, depending on the storage.
     */
    void onLogout(ServerPlayer player);

    /**
     * @return the player, or {@code null} if the player is not online
     */
    @Nullable
    default ServerPlayer entity() {
        return this.safeEntity().orElse(null);
    }

    /**
     * @return the player, or an empty optional if the player is not online
     */
    default Optional<ServerPlayer> safeEntity() {
        return this.reference().entity();
    }

    /**
     * @return the reference to the player of this data
     */
    default PlayerReference reference() {
        return PlayerReference.get(this);
    }

    /**
     * Sends the message to the player. See {@link PlayerReference#send(Object)}.
     *
     * @param message the message to send
     */
    default void send(Object message) {
        this.reference().send(message);
    }

    /**
     * Gets the UUID of the player as a string. Implementations can override this to return the player name.
     *
     * @return the UUID of this instance as a string
     */
    @Override
    default String name() {
        return this.uuid().toString();
    }
}
