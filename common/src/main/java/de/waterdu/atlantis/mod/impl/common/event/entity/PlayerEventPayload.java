package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.entity.player.Player;

/**
 * Holds the data of an event about a player.
 */
public abstract class PlayerEventPayload implements EventPayload {

    private final Player player;

    /**
     * Creates the payload for a player event.
     *
     * @param player the player of the event
     */
    protected PlayerEventPayload(Player player) {
        this.player = player;
    }

    /**
     * @return the player of the event
     */
    public Player player() {
        return this.player;
    }

    /**
     * Holds the data of {@link UEvent#LOG_IN}, which fires when a player logs in.
     */
    public static class LogIn extends PlayerEventPayload {

        /**
         * Creates the payload for a player that logs in.
         *
         * @param player the player that logs in
         */
        public LogIn(Player player) {
            super(player);
        }

        /**
         * @return {@link UEvent#LOG_IN}
         */
        @Override
        public Key<?> type() {
            return UEvent.LOG_IN;
        }
    }

    /**
     * Holds the data of {@link UEvent#LOG_OUT}, which fires when a player logs out.
     */
    public static class LogOut extends PlayerEventPayload {

        /**
         * Creates the payload for a player that logs out.
         *
         * @param player the player that logs out
         */
        public LogOut(Player player) {
            super(player);
        }

        /**
         * @return {@link UEvent#LOG_OUT}
         */
        @Override
        public Key<?> type() {
            return UEvent.LOG_OUT;
        }
    }
}
