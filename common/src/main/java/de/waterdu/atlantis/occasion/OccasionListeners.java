package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.server.ServerTickEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.InteractionResult;

public final class OccasionListeners {

    public OccasionListeners() {
        Mods.modLoader().event(UEvent.LOG_IN).listen(this::onLogin);
        Mods.modLoader().event(UEvent.LOG_OUT).listen(this::onLogout);
        Mods.modLoader().event(UEvent.SERVER_TICK_POST).listen(this::onTick);
    }

    public InteractionResult onLogin(PlayerEventPayload.LogIn event) {
        var reference = PlayerReference.get(event.player());
        var recovery = OccasionRecovery.pop(reference);
        var occasion = OccasionManager.getFor(reference);
        var recover = recovery != null;
        if (recover && occasion.isPresent() && occasion.get().currentMoment.get() == recovery.getA()) {
            recover = false;
            occasion.get().teleportToStart(reference);
        }
        if (recover) {
            recovery.getB().teleport(reference);
        } else {
            OccasionManager.handleEvent(event);
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onLogout(PlayerEventPayload.LogOut event) {
        OccasionManager.handleEvent(event);
        return InteractionResult.PASS;
    }

    public InteractionResult onTick(ServerTickEventPayload.Post event) {
        OccasionManager.tick();
        return InteractionResult.PASS;
    }
}
