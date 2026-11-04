package de.waterdu.atlantis.mixin.network;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.network.PacketInterceptEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.network.CancelledPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emits INTERCEPT_PACKET for packets that a player connection sends or handles. Listeners can change or cancel the
 * packet.
 */
@Mixin(value = Connection.class)
public abstract class MixinConnection {

    @Shadow private PacketListener packetListener;

    /**
     * @author Water
     * @reason To handle packet interception.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
        packet = (Packet<T>) atlantis$interceptPacketStatic(packet, listener);
        if (!atlantis$isPacketCancelled(packet)) {
            packet.handle((T) listener);
        }
    }

    /**
     * Replaces the packet to send with the packet from {@link #atlantis$interceptPacket}.
     *
     * @param packet the packet to send
     * @return the changed packet, or {@link CancelledPacket#INSTANCE} if a listener canceled it
     */
    @ModifyVariable(
            method = "doSendPacket",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    public Packet<?> modifyPacketB(Packet<?> packet) {
        return atlantis$interceptPacket(packet);
    }

    /**
     * Stops the send if the packet is {@link CancelledPacket#INSTANCE}.
     *
     * @param packet the packet to send
     * @param listener the listener for the send result
     * @param flush {@code true} to flush the channel
     * @param ci the callback info
     */
    @Inject(
            method = "doSendPacket",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void onDoSendPacket(Packet<?> packet, PacketSendListener listener, boolean flush, CallbackInfo ci) {
        if (atlantis$isPacketCancelled(packet)) {
            ci.cancel();
        }
    }

    /**
     * Emits INTERCEPT_PACKET for this connection if its listener is a player connection.
     *
     * @param packet the packet
     * @return the changed packet, {@link CancelledPacket#INSTANCE} if a listener canceled it, or the same packet
     */
    @Unique
    private Packet<?> atlantis$interceptPacket(Packet<?> packet) {
        if (this.packetListener instanceof ServerPlayerConnection spc) {
            var event = Mods.modLoader().event(UEvent.INTERCEPT_PACKET).emit(new PacketInterceptEventPayload((Connection) (Object) this, spc.getPlayer(), packet));
            if (event.cancelled()) {
                return CancelledPacket.INSTANCE;
            } else if (event.isPacketChanged()) {
                return event.packet();
            }
        }
        return packet;
    }

    /**
     * Emits INTERCEPT_PACKET for a handled packet if the listener is a player connection. Gets the connection from the
     * packet listener of the player.
     *
     * @param packet the packet
     * @param listener the listener that handles the packet
     * @return the changed packet, {@link CancelledPacket#INSTANCE} if a listener canceled it, or the same packet
     */
    @Unique
    private static Packet<?> atlantis$interceptPacketStatic(Packet<?> packet, PacketListener listener) {
        if (listener instanceof ServerPlayerConnection spc) {
            var connection = ((MixinServerCommonPacketListenerImpl) (Object) spc.getPlayer().connection).atlantis$getConnection();
            var event = Mods.modLoader().event(UEvent.INTERCEPT_PACKET).emit(new PacketInterceptEventPayload(connection, spc.getPlayer(), packet));
            if (event.cancelled()) {
                return CancelledPacket.INSTANCE;
            } else if (event.isPacketChanged()) {
                return event.packet();
            }
        }
        return packet;
    }

    /**
     * Checks if the packet is {@link CancelledPacket#INSTANCE}.
     *
     * @param packet the packet
     * @return {@code true} if the packet is canceled
     */
    @Unique
    private static boolean atlantis$isPacketCancelled(Packet<?> packet) {
        return packet == CancelledPacket.INSTANCE;
    }
}
