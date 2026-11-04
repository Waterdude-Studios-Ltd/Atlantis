package de.waterdu.atlantis.mod.impl.common.event.player;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fires on the server thread after the server validates a chat message and before it broadcasts the message. If you
 * cancel the event, the server does not broadcast the message, and the player's signed message chain stays intact.
 */
public class ChatEventPayload implements EventPayload, Cancellable {

    private final ServerPlayer player;
    private final PlayerChatMessage message;
    private boolean cancelled = false;

    /**
     * Creates the payload for a chat message.
     *
     * @param player the player who sends the message
     * @param message the validated chat message
     */
    public ChatEventPayload(ServerPlayer player, PlayerChatMessage message) {
        this.player = player;
        this.message = message;
    }

    /**
     * @return {@link UEvent#CHAT}
     */
    @Override
    public Key<?> type() {
        return UEvent.CHAT;
    }

    /**
     * @return the player who sends the message
     */
    public ServerPlayer player() {
        return this.player;
    }

    /**
     * @return the validated chat message
     */
    public PlayerChatMessage message() {
        return this.message;
    }

    /**
     * @return the signed content of the message as plain text
     */
    public String text() {
        return this.message.signedContent();
    }

    /**
     * Cancels the event. The server then does not broadcast the message.
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
