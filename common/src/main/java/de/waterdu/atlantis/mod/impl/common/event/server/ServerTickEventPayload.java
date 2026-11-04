package de.waterdu.atlantis.mod.impl.common.event.server;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.MinecraftServer;

/**
 * Server tick events. The loader hooks fire {@link Pre} before each server tick and {@link Post} after it.
 */
public abstract class ServerTickEventPayload implements EventPayload {

    private final MinecraftServer server;

    /**
     * Creates the payload of a tick event.
     *
     * @param server the server that ticks
     */
    protected ServerTickEventPayload(MinecraftServer server) {
        this.server = server;
    }

    /**
     * @return the server that ticks
     */
    public MinecraftServer server() {
        return this.server;
    }

    /**
     * Holds the data of {@link UEvent#SERVER_TICK_PRE}, which fires before each server tick.
     */
    public static class Pre extends ServerTickEventPayload {

        /**
         * Creates the payload that fires before a server tick.
         *
         * @param server the server that ticks
         */
        public Pre(MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_TICK_PRE}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_TICK_PRE;
        }
    }

    /**
     * Holds the data of {@link UEvent#SERVER_TICK_POST}, which fires after each server tick.
     */
    public static class Post extends ServerTickEventPayload {

        /**
         * Creates the payload that fires after a server tick.
         *
         * @param server the server that ticks
         */
        public Post(MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_TICK_POST}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_TICK_POST;
        }
    }
}
