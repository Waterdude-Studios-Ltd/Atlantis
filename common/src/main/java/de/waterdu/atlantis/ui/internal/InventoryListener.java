package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.server.ServerTickEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.world.InteractionResult;

public class InventoryListener {

    private static long tick = 0;

    private InventoryListener() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {
        Mods.modLoader().event(UEvent.SERVER_TICK_PRE).listen(InventoryListener::onServerTick);
    }

    private static InteractionResult onServerTick(ServerTickEventPayload.Pre event) {
        InventoryListener.tick++;
        for (var entry : AtlantisUI.FUTURE_OPEN.entrySet()) {
            var uuid = entry.getKey();
            var page = entry.getValue();
            var player = PlayerReference.get(uuid).entityDirect();
            if (player == null || Mods.modLoader().isFakePlayer(player)) {
                continue;
            }
            if (page.closureMarker()) {
                var windowId = player.containerMenu == null ? 0 : player.containerMenu.containerId;
                var closeWindowPacket = new ClientboundContainerClosePacket(windowId);
                player.closeContainer();
                player.connection.send(closeWindowPacket);
                player.getInventory().setChanged();
                AtlantisUI.FUTURE_OPEN.remove(uuid);
                continue;
            }
            if (player.containerMenu == player.inventoryMenu) {
                AtlantisUI.open(player, page);
                AtlantisUI.FUTURE_OPEN.remove(uuid);
            }
        }

        for (var player : event.server().getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof AtlantisContainerMenuInternal container) {
                var refreshRate = container.getAquaUIInventory().options().refreshRate();
                if (refreshRate < 2 || InventoryListener.tick % refreshRate == 0) {
                    container.getAquaUIInventory().onUpdate(player, container);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
