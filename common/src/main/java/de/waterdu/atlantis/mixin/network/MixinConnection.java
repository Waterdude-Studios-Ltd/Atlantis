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

    @ModifyVariable(
            method = "doSendPacket",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    public Packet<?> modifyPacketB(Packet<?> packet) {
        return atlantis$interceptPacket(packet);
    }

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

    @Unique
    private static boolean atlantis$isPacketCancelled(Packet<?> packet) {
        return packet == CancelledPacket.INSTANCE;
    }
}
