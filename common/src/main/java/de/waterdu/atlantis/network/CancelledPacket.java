package de.waterdu.atlantis.network;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.NonNull;

public final class CancelledPacket implements Packet<PacketListener> {

    public static final CancelledPacket INSTANCE = new CancelledPacket();

    private CancelledPacket() {
        // noop
    }

    @Override
    public @NonNull PacketType<? extends Packet<PacketListener>> type() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handle(@NonNull PacketListener packetListener) {
        throw new UnsupportedOperationException();
    }
}
