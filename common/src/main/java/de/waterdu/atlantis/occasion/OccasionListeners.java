package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.server.ServerTickEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.InteractionResult;

/**
 * Listens for login, logout and server tick events, and passes them to {@link OccasionManager}.
 */
public final class OccasionListeners {

    /**
     * Creates the listeners. {@link OccasionManager} holds the only instance.
     */
    OccasionListeners() {
        // noop
    }

    /**
     * Subscribes the listeners to {@code LOG_IN}, {@code LOG_OUT} and {@code SERVER_TICK_POST}.
     */
    public void register() {
        Mods.modLoader().event(UEvent.LOG_IN).listen(this::onLogin);
        Mods.modLoader().event(UEvent.LOG_OUT).listen(this::onLogout);
        Mods.modLoader().event(UEvent.SERVER_TICK_POST).listen(this::onTick);
    }

    /**
     * Teleports the player to their stored recovery position, if they have one. If the player is still in an
     * occasion at the stored moment, teleports them to the start of that moment instead. When there is no recovery,
     * passes the event to all occasions.
     *
     * @param event the login event
     * @return {@link InteractionResult#PASS}
     */
    public InteractionResult onLogin(PlayerEventPayload.LogIn event) {
        var reference = PlayerReference.get(event.player());
        var recovery = OccasionRecovery.pop(reference);
        var occasion = OccasionManager.getFor(reference);
        var recover = recovery != null;

        if (recover && occasion != null && occasion.currentMoment.get() == recovery.getA()) {
            recover = false;
            occasion.teleportToStart(reference);
        }

        if (recover) {
            recovery.getB().teleport(reference);
        } else {
            OccasionManager.handleEvent(event);
        }

        return InteractionResult.PASS;
    }

    /**
     * Passes the logout event to all occasions.
     *
     * @param event the logout event
     * @return {@link InteractionResult#PASS}
     */
    public InteractionResult onLogout(PlayerEventPayload.LogOut event) {
        OccasionManager.handleEvent(event);
        return InteractionResult.PASS;
    }

    /**
     * Ticks all occasions after each server tick.
     *
     * @param event the server tick event
     * @return {@link InteractionResult#PASS}
     */
    public InteractionResult onTick(ServerTickEventPayload.Post event) {
        OccasionManager.tick();
        return InteractionResult.PASS;
    }
}
