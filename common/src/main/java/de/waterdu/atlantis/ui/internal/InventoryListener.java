package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class InventoryListener {

    private static long tick = 0;

    private InventoryListener() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {

    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        this.tick++;
        for (var entry : AtlantisUI.FUTURE_OPEN.entrySet()) {
            var uuid = entry.getKey();
            var page = entry.getValue();
            var player = PlayerReference.get(uuid).entityDirect();
            if (player == null || player instanceof FakePlayer) {
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
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.containerMenu instanceof AtlantisContainerMenuInternal container) {
                var refreshRate = container.getAquaUIInventory().options().refreshRate();
                if (refreshRate < 2 || this.tick % refreshRate == 0) {
                    container.getAquaUIInventory().onUpdate(player, container);
                }
            }
        }
    }
}
