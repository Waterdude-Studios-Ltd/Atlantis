package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class OccasionListeners {

    public OccasionListeners() {
        NeoForge.EVENT_BUS.register(this);
        Mods.modLoader().event(UEvent.LOG_IN).listen(this::onLogin);
        Mods.modLoader().event(UEvent.LOG_OUT).listen(this::onLogout);
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

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        OccasionManager.tick();
    }
}
