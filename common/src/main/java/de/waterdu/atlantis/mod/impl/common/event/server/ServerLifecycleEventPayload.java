package de.waterdu.atlantis.mod.impl.common.event.server;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Server lifecycle events. On NeoForge, {@link Starting} fires during dedicated server setup, before the server
 * exists. Thus, {@link #server()} is null there. The configs are loaded before {@link Started} fires.
 */
public abstract class ServerLifecycleEventPayload implements EventPayload {

    private final @Nullable MinecraftServer server;

    /**
     * Creates the payload of a lifecycle event.
     *
     * @param server the server, or {@code null} if it does not exist yet
     */
    protected ServerLifecycleEventPayload(@Nullable MinecraftServer server) {
        this.server = server;
    }

    /**
     * @return the server, or {@code null} if it does not exist yet
     */
    public @Nullable MinecraftServer server() {
        return this.server;
    }

    /**
     * Holds the data of {@link UEvent#SERVER_STARTING}, which fires when the server starts.
     */
    public static class Starting extends ServerLifecycleEventPayload {

        /**
         * Creates the payload that fires when the server starts.
         *
         * @param server the server, or {@code null} if it does not exist yet
         */
        public Starting(@Nullable MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_STARTING}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_STARTING;
        }
    }

    /**
     * Holds the data of {@link UEvent#SERVER_STARTED}, which fires after the server has started.
     */
    public static class Started extends ServerLifecycleEventPayload {

        /**
         * Creates the payload that fires after the server has started.
         *
         * @param server the server
         */
        public Started(@Nullable MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_STARTED}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_STARTED;
        }
    }

    /**
     * Holds the data of {@link UEvent#SERVER_STOPPING}, which fires when the server starts to stop.
     */
    public static class Stopping extends ServerLifecycleEventPayload {

        /**
         * Creates the payload that fires when the server starts to stop.
         *
         * @param server the server
         */
        public Stopping(@Nullable MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_STOPPING}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_STOPPING;
        }
    }

    /**
     * Holds the data of {@link UEvent#SERVER_STOPPED}, which fires after the server has stopped.
     */
    public static class Stopped extends ServerLifecycleEventPayload {

        /**
         * Creates the payload that fires after the server has stopped.
         *
         * @param server the server
         */
        public Stopped(@Nullable MinecraftServer server) {
            super(server);
        }

        /**
         * @return {@link UEvent#SERVER_STOPPED}
         */
        @Override
        public Key<?> type() {
            return UEvent.SERVER_STOPPED;
        }
    }
}
