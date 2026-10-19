package de.waterdu.atlantis.mod.impl.common.event.server;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.MinecraftServer;

public abstract class ServerTickEventPayload implements EventPayload {

    private final MinecraftServer server;

    protected ServerTickEventPayload(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return this.server;
    }

    public static class Pre extends ServerTickEventPayload {

        public Pre(MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_TICK_PRE;
        }
    }

    public static class Post extends ServerTickEventPayload {

        public Post(MinecraftServer server) {
            super(server);
        }

        @Override
        public Key<?> type() {
            return UEvent.SERVER_TICK_POST;
        }
    }
}
