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

    protected ServerLifecycleEventPayload(@Nullable MinecraftServer server) {
        this.server = server;
    }

    public @Nullable MinecraftServer server() {
        return this.server;
    }

    public static class Starting extends ServerLifecycleEventPayload {

        public Starting(@Nullable MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_STARTING;
        }
    }

    public static class Started extends ServerLifecycleEventPayload {

        public Started(@Nullable MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_STARTED;
        }
    }

    public static class Stopping extends ServerLifecycleEventPayload {

        public Stopping(@Nullable MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_STOPPING;
        }
    }

    public static class Stopped extends ServerLifecycleEventPayload {

        public Stopped(@Nullable MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_STOPPED;
        }
    }
}
