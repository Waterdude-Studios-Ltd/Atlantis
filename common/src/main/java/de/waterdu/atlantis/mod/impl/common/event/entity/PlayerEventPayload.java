package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.entity.player.Player;

public abstract class PlayerEventPayload implements EventPayload {

    private final Player player;

    protected PlayerEventPayload(Player player) {
        this.player = player;
    }

    public Player player() {
        return this.player;
    }

    public static class LogIn extends PlayerEventPayload {

        public LogIn(Player player) {
            super(player);
        }

        @Override
        public Key<?> type() {
            return UEvent.LOG_IN;
        }
    }

    public static class LogOut extends PlayerEventPayload {

        public LogOut(Player player) {
            super(player);
        }

        @Override
        public Key<?> type() {
            return UEvent.LOG_OUT;
        }
    }
}
