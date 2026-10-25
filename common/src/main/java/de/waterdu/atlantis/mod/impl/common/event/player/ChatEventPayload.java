package de.waterdu.atlantis.mod.impl.common.event.player;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fires on the server thread after a chat message has been validated and before it is broadcast. Cancelling it
 * stops the broadcast without breaking the player's signed message chain.
 */
public class ChatEventPayload implements EventPayload, Cancellable {

    private final ServerPlayer player;
    private final PlayerChatMessage message;
    private boolean cancelled = false;

    public ChatEventPayload(ServerPlayer player, PlayerChatMessage message) {
        this.player = player;
        this.message = message;
    }

    @Override
    public Key<?> type() {
        return UEvent.CHAT;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public PlayerChatMessage message() {
        return this.message;
    }

    public String text() {
        return this.message.signedContent();
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
