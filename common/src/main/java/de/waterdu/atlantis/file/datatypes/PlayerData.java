package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.interfaces.UniquelyNamed;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Optional;

public interface PlayerData extends NamedData, UniquelyNamed {

    void onLogin(ServerPlayer player);

    void onLogout(ServerPlayer player);

    @Nullable
    default ServerPlayer entity() {
        return this.safeEntity().orElse(null);
    }

    default Optional<ServerPlayer> safeEntity() {
        return this.reference().entity();
    }

    default PlayerReference reference() {
        return PlayerReference.get(this);
    }

    default void send(Object message) {
        this.reference().send(message);
    }

    @Override
    default String name() {
        return this.uuid().toString();
    }
}
